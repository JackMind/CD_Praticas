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
    public void askData(SpreadMessage spreadMessage) throws SpreadException {
        if(this.isSelfMessage(spreadMessage)){
            return;
        }
        AskData askData = (AskData) spreadMessage.getObject();
        String key = askData.getKey();
        log.info("{} está a procura da informacao [{}]", spreadMessage.getSender(), key);
        Optional<DataEntity> dataEntity = this.database.findById(key);
        if(dataEntity.isPresent()){
            DataEntity localData = dataEntity.get();
            if ( localData.getInvalidate() == null || localData.getInvalidate().equals(Boolean.FALSE) ) {
                log.info("Eu tenho a informação: {}, vou enviar para: {}", dataEntity.get(), spreadMessage.getSender());
                try{
                    connection.multicast(createUnicastMessage(new AskDataResponse(dataEntity.get()),spreadMessage.getSender() ));
                }catch (SpreadException exception){
                    log.error("Error multicasting message: askDataResponse ", exception);
                }
            }
            if( localData.getInvalidate() != null ){
                log.info("Informação [{}] é invalida localmente.", key);
            }
        }else {
            log.info("Nao tenho [{}]", key);
        }
    }

    @Override
    public void askDataResponse(SpreadMessage spreadMessage) throws SpreadException {
        AskDataResponse askDataResponse = (AskDataResponse) spreadMessage.getObject();
        String key = askDataResponse.getDataDto().getKey();

        WaitingDataReadCallback callback = waitingData.get(key);
        if(callback != null){
            log.info("Recebi resposta da informacao que pedi: {}", askDataResponse.getDataDto());
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
                if(cardinality > max && cardinality != this.getServerNameCardinality(this.connection.getPrivateGroup())){
                    max = cardinality;
                    leaderIndex = index;
                }
            }

            SpreadGroup leader = info.getMembers()[leaderIndex];
            log.info("Elegi o coordenador {} com cardinalidade: {}", leader, max);

            List<DataEntity.DataDto> dataDtos = new ArrayList<>();
            this.database.findAll().forEach( dataEntity -> {
                dataEntity.setInvalidate(true);
                dataDtos.add(new DataEntity.DataDto(dataEntity));
                this.database.save(dataEntity);
            });
            log.info("Toda a informacao local foi invalidada");

            log.info("Vou pedir startup para a informacao: {}", dataDtos);
            connection.multicast(createUnicastMessage(new StartupRequestUpdate(dataDtos), leader));

        }
    }

    @Override
    public void startupData(SpreadMessage spreadMessage) throws SpreadException {
        log.info("COORDENADOR=={}: Recebi o pedido de ajuda para o startup de: {}", this.me, spreadMessage.getSender());
        StartupRequestUpdate startupRequestUpdate = (StartupRequestUpdate) spreadMessage.getObject();

        List<DataEntity.DataDto> startupResponse = new ArrayList<>();
        startupRequestUpdate.getDataDtoList().forEach(dataDto -> {
            Optional<DataEntity> dataEntity = this.database.findById(dataDto.getKey());
            dataEntity.ifPresent(entity -> startupResponse.add(new DataEntity.DataDto(entity)));
        });

        log.info("Vou devolver a seguinte informacao de startup: {}", startupResponse);
        connection.multicast(createUnicastMessage(new StartupResponseUpdate(startupResponse), spreadMessage.getSender()));
    }

    @Override
    public void startupDataResponse(SpreadMessage spreadMessage) throws SpreadException {
        StartupResponseUpdate startupResponseUpdate = (StartupResponseUpdate) spreadMessage.getObject();
        log.info("Recebi informacao de startup do leader: {} = {}", spreadMessage.getSender(), startupResponseUpdate);
        startupResponseUpdate.getDataDtoList().forEach(dataDto -> {
            Optional<DataEntity> dataEntity = this.database.findById(dataDto.getKey());
            dataEntity.ifPresent(newLocalEntity -> {
                newLocalEntity.setData(new DataEntity.Data(dataDto.getData()) );
                this.database.save(newLocalEntity);
                log.info("Validada: {}", newLocalEntity);
            });
        });

        List<String> allLocalKeys = new ArrayList<>();
        this.database.findAll().forEach(dataEntity -> allLocalKeys.add(dataEntity.getKey()));
        allLocalKeys.forEach(this::deleteIfExistsAndIsInvalidated);

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

        log.info("Resposta de {} para poder escrever a key: [{}].", spreadMessage.getSender(), key);
        if(wantToWriteData.containsKey(key)){
            WantToWriteData data = wantToWriteData.get(key);
            data.increment();
            log.info("Resposta recebidas para key: [{}] -> {}", key, data.getReceivedResponses());
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

        if(this.isSelfMessage(spreadMessage)){
            log.debug("Sou eu proprio, LOL");
            return;
        }

        if(!wantToWriteData.containsKey(key)){
            log.info("O {} quer escrever a key: [{}]. OK!", spreadMessage.getSender(), key);
            executeInvalidateData(key);
            if(!test_conflict){
                try{
                    connection.multicast(createUnicastMessage(new WantToWriteResponse(key), spreadMessage.getSender()));
                } catch (SpreadException spreadException) {
                    log.error("Error multicasting message: wantToWriteReceived ", spreadException);
                }
            }
        }else {
            log.warn("Eu estou no processo de escrever para a key: [{}], FIGHT!", key);
            try{
                connection.multicast(createMulticastMessage(new WriteConflict(data)));
            } catch (SpreadException spreadException){
                log.error("Error multicasting message: sendAppendDataToParticipants ", spreadException);
            }
            wantToWriteData.get(key).getCallback().conflict(spreadMessage.getSender());
        }
    }

    private void executeInvalidateData(String key){
        Optional<DataEntity> dataEntity = this.database.findById(key);
        if(dataEntity.isPresent()){
            DataEntity dataEntity1 = dataEntity.get();

            dataEntity1.setInvalidate(true);

            this.database.save(dataEntity1);
            log.info("Informacao com key: [{}] invalidada.", key);
        }else{
            log.info("Nao tenho a key [{}]", key);
        }
    }

    @Override
    public void conflictReceived(SpreadMessage spreadMessage) throws SpreadException {
        if(this.isSelfMessage(spreadMessage)){
            log.debug("Mensagem de conflict comigo mesmo.");
            return;
        }
        WriteConflict writeConflict = (WriteConflict) spreadMessage.getObject();
        String key = writeConflict.getDataDto().getKey();

        if(wantToWriteData.containsKey(key)){
            log.info("Conflicto recebido para a key: [{}] do {}", key, spreadMessage.getSender());
            wantToWriteData.get(key).getCallback().conflict(spreadMessage.getSender());
        }
    }

    @Override
    public void dataWritten(SpreadMessage spreadMessage) throws SpreadException {
        if(isSelfMessage(spreadMessage)){
            return;
        }
        DataWritten dataWritten = (DataWritten) spreadMessage.getObject();
        String key = dataWritten.getKey();

        log.info("Informacao key: [{}] foi escrita com sucesso a eliminar replica local", key);
        deleteIfExistsAndIsInvalidated(key);
    }

    @Override
    public void revalidateData(SpreadMessage spreadMessage) throws SpreadException {
        if(isSelfMessage(spreadMessage)){
            return;
        }
        RevalidateData revalidateData = (RevalidateData) spreadMessage.getObject();
        String key = revalidateData.getKey();

        Optional<DataEntity> localData = this.database.findById(key);
        if(localData.isPresent()){
            DataEntity data = localData.get();
            data.setInvalidate(false);
            this.database.save(data);
            log.info("Informacao com key: [{}] revalidada.", key);
        }
    }

    public boolean isServerStartingUp(){return waitStartupDataUpdate;}

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
            log.info("Informacao pedida recebida : {}", dataDto);
            response.set(dataDto);
            responseReceived.set(true);
        };

        try{
            log.info("Vou pedir a todos, se alguem tem: [{}]", key );
            connection.multicast(createMulticastMessage(new AskData(key)));

            waitingData.put(key, waitingDataReadCallback);

            try{
                waitForResponse(responseReceived);
                if(response.get() != null){
                    DataEntity newLocalData = new DataEntity(response.get());
                    this.database.saveAndFlush(newLocalData);
                    log.info("Guardei uma copia local de: {}", newLocalData);

                }
            }catch (TimeoutException exception){
                log.info("Nao recebi resposta para a informacao que pedi.");
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

        try {
            final AtomicBoolean responseReceived = new AtomicBoolean(false);
            final AtomicReference<SpreadGroup> conflict = new AtomicReference<>(null);

            WaitingAllResponses waitingAllResponses = new WaitingAllResponses() {
                @Override
                public void allResponsesReceived() {
                    log.info("Recebi confirmacao para para escrever a key: [{}] de todos os participantes", key);
                    responseReceived.set(true);
                }

                @Override
                public void conflict(SpreadGroup conflictSender) {
                    log.info("Conflicto com {}!", conflictSender);
                    conflict.set(conflictSender);
                }
            };

            wantToWriteData.put(key, new WantToWriteData(waitingAllResponses));

            log.info("Vou perguntar aos participantes se posso escrever.");
            connection.multicast(createMulticastMessage(new WantToWrite(dataDto)));

            try{
                waitForResponse(responseReceived, conflict);
                saveDataLocally(dataDto);
                log.info("Replica {} guardada localmente.", dataDto);
                success = true;
            }catch (TimeoutException exception){
                log.info("Nem todos os participantes responderam, abortar escrita.");
                sendRevalidateData(key);
                success = false;
            }catch(ConflictException conflictException){
                success = handleConflict(conflict.get(), dataDto);
            } finally{
                wantToWriteData.remove(key);
            }


        } catch (SpreadException | InterruptedException spreadException) {
            spreadException.printStackTrace();
            success = false;
        }
        return success;
    }

    private boolean handleConflict(SpreadGroup conflictWith, DataEntity.DataDto dataDto){
        if(electLeader(conflictWith)){
            saveDataLocally(dataDto);
            log.info("Informação {} salva localmente, apos conflito com: {}.", dataDto, conflictWith);
            return true;
        }else{
            deleteIfExists(dataDto.getKey());
            return false;
        }
    }

    private void deleteIfExists(String key){
        if(this.database.findById(key).isPresent()){
            this.database.deleteById(key);
        }
    }

    private void deleteIfExistsAndIsInvalidated(String key){
        Optional<DataEntity> dataEntity = this.database.findById(key);
        if(dataEntity.isPresent() && dataEntity.get().getInvalidate().equals(Boolean.TRUE)){
            this.database.deleteById(key);
            log.info("Informacao removida localmente: [{}]", key);
        }
    }

    private void sendRevalidateData(String key) {
        log.info("A enviar mensagem para revalidacao da key: [{}]", key);
        try{
            connection.multicast(createMulticastMessage( new RevalidateData(key) ));
        }catch (SpreadException exception){
            log.error("Error multicasting message: sendRevalidateData ", exception);
        }
    }

    private void saveDataLocally(DataEntity.DataDto dataDto){
        this.database.save(new DataEntity(dataDto));
        sendDataWritten(dataDto.getKey());
    }
    private void sendDataWritten(String key) {
        log.info("A enviar confirmacao de escrita da key: [{}]", key);
        try{
            connection.multicast(createMulticastMessage( new DataWritten(key) ));
        }catch (SpreadException exception){
            log.error("Error multicasting message: sendDataWritten ", exception);
        }
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
            log.info("Eu nao vou escrever, eu tenho a cardinalidade mais pequena");
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

    private boolean isSelfMessage(SpreadMessage spreadMessage){
        return this.connection.getPrivateGroup().equals(spreadMessage.getSender());
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
