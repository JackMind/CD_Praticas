package com.isel.cd.server;


import com.isel.cd.server.messages.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spread.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class LeaderManager implements SpreadMessageListenerInterface {

    private final DatabaseRepository database;
    private final SpreadConnection connection;
    private final String groupId;
    private final SpreadGroup me;
    private final int timeoutInSec;
    private final boolean local;
    private final boolean test_conflict;

    private final Map<String, WRITE_STATE> writeLocks = new HashMap<>();
    private final Map<String, WaitingDataReadCallback> waitingData = new HashMap<>();
    private final Map<String, WantToWriteData> wantToWriteData = new HashMap<>();


    public LeaderManager(final SpreadConnection connection, final String groupId, final DatabaseRepository database, final int timeoutInSec, final boolean local, final boolean test_conflict) {
        this.connection = connection;
        this.me = connection.getPrivateGroup();
        this.groupId = groupId;
        this.database = database;
        this.timeoutInSec = timeoutInSec;
        this.local = local;
        this.test_conflict = test_conflict;
    }

    private int numberOfParticipants = -1;
    private volatile boolean waitStartupDataUpdate = true;

    @Override
    public void askDataResponse(SpreadMessage spreadMessage) throws SpreadException {
        AskData askData = (AskData) spreadMessage.getObject();
        log.info("{} está a procura da informação key: {}", spreadMessage.getSender(), askData.getKey());
        Optional<DataEntity> dataEntity = this.database.findById(askData.getKey());
        if (dataEntity.isPresent()) {
            log.info("Eu tenho a informação: {}, vou enviar para: {}", dataEntity.get(), spreadMessage.getSender());
            try{
                connection.multicast(createUnicastMessage(new AskDataResponse(dataEntity.get()),spreadMessage.getSender() ));
            }catch (SpreadException exception){
                log.error("Error multicasting message: askDataResponse ", exception);
            }
        }
    }

    @Override
    public void askDataResponseReceived(SpreadMessage spreadMessage) throws SpreadException {
        AskDataResponse askDataResponse = (AskDataResponse) spreadMessage.getObject();
        WaitingDataReadCallback callback = waitingData.get(askDataResponse.getDataDto().getKey());
        if(callback != null){
            log.info("Recebi resposta da informação que pedi: {}", askDataResponse.getDataDto());
            callback.dataReceived(askDataResponse.getDataDto());
        }
    }

    @Override
    public void checkStartup(MembershipInfo info) throws SpreadException {
        if(waitStartupDataUpdate){
            if(info.getMembers().length == 2){
                log.info("Sou o primeiro");
                return;
            }
            log.info("A eleger coordenador para me ajudar no startup.");
            int max = 0;
            int leaderIndex = 0;
            for(int index = 0; index < info.getMembers().length; index++){
                if(this.getServerName(info.getMembers()[index]).equals("Service")){
                    continue;
                }
                int cardinality = this.getServerNameCardinality(info.getMembers()[index]);
                if(cardinality > max){
                    max = cardinality;
                    leaderIndex = index;
                }
            }

            SpreadGroup leader = info.getMembers()[leaderIndex];
            log.info("Elegi o coordenador {} com cardinalidade: {}", leader, max);

            List<DataEntity.DataDto> dataDtos = new ArrayList<>();
            this.database.findAll().forEach( dataEntity -> dataDtos.add(new DataEntity.DataDto(dataEntity)));

            log.info("Vou pedir startup para a informação: {}", dataDtos);
            connection.multicast(createUnicastMessage(new StartupRequestUpdate(dataDtos), leader));

        }
    }

    @Override
    public void sendStartupData(SpreadMessage spreadMessage) throws SpreadException {
        log.info("COORDENADOR=={}: Recebi o pedido de ajuda para o startup de: {}", this.me, spreadMessage.getSender());
        StartupRequestUpdate startupRequestUpdate = (StartupRequestUpdate) spreadMessage.getObject();

        List<DataEntity.DataDto> startupResponse = new ArrayList<>();
        startupRequestUpdate.getDataDtoList().forEach(dataDto -> {
            Optional<DataEntity> dataEntity = this.database.findById(dataDto.getKey());
            dataEntity.ifPresent(entity -> startupResponse.add(new DataEntity.DataDto(entity)));
        });

        log.info("Vou devolver a seguinte informação de startup: {}", startupResponse);
        connection.multicast(createUnicastMessage(new StartupResponseUpdate(startupResponse), spreadMessage.getSender()));
    }

    @Override
    public void startupDataReceived(SpreadMessage spreadMessage) throws SpreadException {
        StartupResponseUpdate startupResponseUpdate = (StartupResponseUpdate) spreadMessage.getObject();
        log.info("Recebi informação de startup do leader: {} com informação: {}", spreadMessage.getSender(), startupResponseUpdate);
        startupResponseUpdate.getDataDtoList().forEach(dataDto -> {
            Optional<DataEntity> dataEntity = this.database.findById(dataDto.getKey());
            dataEntity.ifPresent(entity -> {
                entity.setData(new DataEntity.Data(dataDto.getData()) );
                this.database.save(entity);
            });
        });
        log.info("Startup completo.");
        waitStartupDataUpdate = false;
    }

    @Override
    public void updateNumberOfParticipants(int numberOfParticipants) {
        this.numberOfParticipants = numberOfParticipants;
    }

    @Override
    public void wantToWriteResponse(SpreadMessage spreadMessage) throws SpreadException {
        String key = ((WantToWriteResponse) spreadMessage.getObject()).getKey();

        log.info("Resposta de {} para poder escrever a key: {}.", spreadMessage.getSender(), key);
        if(wantToWriteData.containsKey(key)){
            WantToWriteData data = wantToWriteData.get(key);
            data.increment();
            log.info("Resposta recebidas para key: {} -> {}", key, data.getReceivedResponses());
            if(data.getReceivedResponses() >= this.numberOfParticipants - 2 /*self e configuration*/){
                data.getCallback().allResponsesReceived();
            }
        }

    }

    @Override
    public void wantToWriteReceived(SpreadMessage spreadMessage) throws SpreadException {
        WantToWrite wantToWrite = (WantToWrite) spreadMessage.getObject();
        DataEntity.DataDto data = wantToWrite.getData();
        String key = data.getKey();

        if(connection.getPrivateGroup().equals(spreadMessage.getSender())){
            log.debug("Sou eu proprio, LOL");
            return;
        }

        if(writeLocks.getOrDefault(key, WRITE_STATE.IDLE).equals(WRITE_STATE.IDLE)){
            log.info("O {} quer escrever a key: {}, OK!", spreadMessage.getSender(), key);
            executeInvalidateData(key);
            if(!test_conflict){
                try{
                    connection.multicast(createUnicastMessage(new WantToWriteResponse(key), spreadMessage.getSender()));
                } catch (SpreadException spreadException) {
                    log.error("Error multicasting message: wantToWriteReceived ", spreadException);
                }
            }
        }else {
            log.warn("Eu estou no processo de escrever para a key: {}, ATENÇÃO!", key);
            try{
                connection.multicast(createMulticastMessage(new WriteConflict(data)));
            } catch (SpreadException spreadException){
                log.error("Error multicasting message: sendAppendDataToParticipants ", spreadException);
            }
            wantToWriteData.get(key).getCallback().conflict(spreadMessage.getSender());
        }
    }

    private void executeInvalidateData(String key){
        if(this.database.findById(key).isPresent()){
            this.database.deleteById(key);
            log.info("Key: {} invalidada.", key);
        }else{
            log.info("Não tinha a key: {}", key);
        }
    }

    @Override
    public void conflictReceived(SpreadMessage spreadMessage) throws SpreadException {
        if(connection.getPrivateGroup().equals(spreadMessage.getSender())){
            log.debug("Mensagem de conflict comigo mesmo.");
            return;
        }
        WriteConflict writeConflict = (WriteConflict) spreadMessage.getObject();
        String key = writeConflict.getDataDto().getKey();

        if(writeLocks.containsKey(key)){
            log.info("Conflicto recebido para a key: {} do {}", key, spreadMessage.getSender());
            wantToWriteData.get(key).getCallback().conflict(spreadMessage.getSender());
        }
    }

    public boolean isStartup(){return waitStartupDataUpdate;}

    private String getServerName(SpreadGroup group){
        return group.toString().split("#")[1];
    }
    private String getServerIp(SpreadGroup group){
        return group.toString().split("#")[2];
    }

    private int getServerNameCardinality(SpreadGroup group){
        String server = this.getServerName(group);
        String ip = this.getServerIp(group);
        return local ? Integer.parseInt(server.substring(server.length()-1))  : Integer.parseInt(server.substring(server.length()-1)) + Integer.parseInt(ip.substring(ip.length()-1));
    }

    private SpreadMessage createMulticastMessage(BaseMessage message){
        SpreadMessage spreadMessage = new SpreadMessage();
        try{
            spreadMessage.setObject(message);
            spreadMessage.addGroup(groupId);
            spreadMessage.setReliable();
        }catch (SpreadException exception){
            log.error("Error creating multicast message:  ", exception);
        }
        return spreadMessage;
    }

    private SpreadMessage createUnicastMessage(BaseMessage message, SpreadGroup group){
        SpreadMessage spreadMessage = new SpreadMessage();
        try{
            spreadMessage.setObject(message);
            spreadMessage.addGroup(group.toString());
            spreadMessage.setReliable();
        }catch (SpreadException exception){
            log.error("Error creating unicast message:  ", exception);
        }
        return spreadMessage;
    }

    public DataEntity.DataDto requestData(String key) {
        AtomicReference<DataEntity.DataDto> response = new AtomicReference<>(null);
        final AtomicBoolean responseReceived = new AtomicBoolean(false);

        WaitingDataReadCallback waitingDataReadCallback = (dataDto) -> {
            log.info("Informação pedida recebida para a data: {} com informação: {}", key, dataDto);
            response.set(dataDto);
            responseReceived.set(true);
        };

        try{
            log.info("Vou pedir a todos, se alguém tem: {}", key );
            connection.multicast(createMulticastMessage(new AskData(key)));

            waitingData.put(key, waitingDataReadCallback);

            try{
                waitForResponse(responseReceived);
            }catch (TimeoutException exception){
                log.info("Não recebi resposta para a informação que pedi.");
                return  null;
            }finally {
                waitingData.remove(key);
            }

        }catch (SpreadException | InterruptedException exception){
            log.error("Error multicasting message: requestData ", exception);
        }

        return response.get();
    }

    public boolean writeData(DataEntity.DataDto dataDto) {
        if(this.numberOfParticipants == 2){
            log.info("Sou o unico no grupo, vou escrever.");
            this.database.save(new DataEntity(dataDto));
        }else {
            return sendWantToWriteMessage(dataDto);
        }
        return true;
    }

    private boolean sendWantToWriteMessage(DataEntity.DataDto dataDto){
        String key = dataDto.getKey();
        boolean success = false;
        writeLocks.put(key, WRITE_STATE.WRITING);

        try {
            final AtomicBoolean responseReceived = new AtomicBoolean(false);
            final AtomicReference<SpreadGroup> conflict = new AtomicReference<>(null);

            WaitingAllResponses waitingAllResponses = new WaitingAllResponses() {
                @Override
                public void allResponsesReceived() {
                    log.info("Recebi confirmação para para escrever a key: {} de todos os participantes", key);
                    responseReceived.set(true);
                }

                @Override
                public void conflict(SpreadGroup conflictSender) {
                    log.info("Conflicto com {}!", conflictSender);
                    writeLocks.put(key, WRITE_STATE.CONFLICT);
                    conflict.set(conflictSender);
                }
            };

            wantToWriteData.put(key, new WantToWriteData(waitingAllResponses));

            log.info("Vou perguntar aos participantes se posso escrever.");
            connection.multicast(createMulticastMessage(new WantToWrite(dataDto)));

            try{
                waitForResponse(responseReceived, conflict);
                this.database.save(new DataEntity(dataDto));
                log.info("Informação {} salva localmente.", dataDto);
            }catch (TimeoutException exception){
                log.info("Nem todos os participantes responderam, abortar escrita.");
                success = false;
            }catch(ConflictException conflictException){
                if(electLeader(conflict.get())){
                    this.database.save(new DataEntity(dataDto));
                    log.info("Informação {} salva localmente.", dataDto);
                    success = true;
                }else{
                    if(this.database.findById(key).isPresent()){
                        this.database.deleteById(key);
                    }
                    success = false;
                }
            } finally{
                writeLocks.put(key, WRITE_STATE.IDLE);
                wantToWriteData.remove(key);
            }


        } catch (SpreadException | InterruptedException spreadException) {
            spreadException.printStackTrace();
            success = false;
        }
        return success;
    }

    private boolean electLeader(SpreadGroup conflictSender){
        SpreadGroup me = this.connection.getPrivateGroup();

        int myCardinality = this.getServerNameCardinality(me);
        int conflictCardinality = this.getServerNameCardinality(conflictSender);
        log.info("My cardinality: {} Conflict cardinality: {}", myCardinality, conflictCardinality);
        if(myCardinality > conflictCardinality){
            log.info("Eu tenho a cardinalidade maior eu vou escrever");
            return true;
        }else{
            log.info("Eu não vou escrever, eu tenho a cardinalidade mais pequena");
            return false;
        }
    }

    private void waitForResponse(final AtomicBoolean responseReceived) throws InterruptedException, TimeoutException {
        long start = System.currentTimeMillis();
        long end = start + (long)timeoutInSec * 1000;
        while(!responseReceived.get()) {
            Thread.sleep(50);
            if(System.currentTimeMillis() > end) {
                log.warn("Timeout reached!");
                throw new TimeoutException();
            }
        }
    }

    private void waitForResponse(final AtomicBoolean responseReceived, final AtomicReference<SpreadGroup> conflict) throws InterruptedException, TimeoutException, ConflictException {
        long start = System.currentTimeMillis();
        long end = start + (long)timeoutInSec * 1000;
        while(!responseReceived.get()) {
            Thread.sleep(50);
            if(System.currentTimeMillis() > end) {
                log.warn("Timeout reached!");
                throw new TimeoutException();
            }
            if(conflict.get() != null){
                log.warn("Conflict!");
                throw new ConflictException();
            }
        }
    }

    public interface WaitingAllResponses {
        void allResponsesReceived();
        void conflict(SpreadGroup conflictSender);
    }

    public interface WaitingDataReadCallback {
        void dataReceived(DataEntity.DataDto dataDto);
    }

    @lombok.Data
    @RequiredArgsConstructor
    public static class WantToWriteData {
        private int receivedResponses = 0;
        private final WaitingAllResponses callback;

        public void increment(){
            receivedResponses++;
        }
    }

}
