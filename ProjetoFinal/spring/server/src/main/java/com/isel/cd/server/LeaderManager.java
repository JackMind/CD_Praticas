package com.isel.cd.server;


import com.isel.cd.server.messages.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import rpcsclientstubs.Data;
import rpcsclientstubs.Key;
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
    private final boolean leaderMechanism;

    public LeaderManager(final SpreadConnection connection, final String groupId, final DatabaseRepository database, final int timeoutInSec, final boolean leaderMechanism) {
        this.connection = connection;
        this.me = connection.getPrivateGroup();
        this.groupId = groupId;
        this.database = database;
        this.timeoutInSec = timeoutInSec;
        this.leaderMechanism = leaderMechanism;
    }

    private SpreadGroup leader;
    private volatile boolean waitStartupDataUpdate = true;

    @Override
    public void notifyServerLeave(SpreadGroup group, MembershipInfo info) {
        if(group.equals(this.leader) ){

            log.info("Leader: {} no longer exists", this.leader);

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

            this.leader = info.getMembers()[leaderIndex];
            log.info("New leader: {} with cardinality: {}", this.leader, max);
        }

        if(amILeader()){
            log.info("LEADER: I'm leader, notify everyone!");
            this.sendIAmLeaderMessage();
        }
    }

    @Override
    public void assignNewLeader(SpreadMessage spreadMessage) throws InterruptedException {
        if(!this.me.equals(spreadMessage.getSender())){
            leader = spreadMessage.getSender();
            log.info("Recebida mensagem de novo leader, novo leader é: {}", leader);
            if(waitStartupDataUpdate){
                this.requestDataUpdate();
            }
        }
    }

    @Async
    void requestDataUpdate() {
        try{
            log.info("Pedido de informação de startup ao leader: {}", this.leader);
            connection.multicast(createUnicastMessage(new StartupRequest(), this.leader));

        }catch (SpreadException exception){
            log.error("Error multicasting message: requestDataUpdate ", exception);
        }
        waitStartupDataUpdate = false;
    }

    @Override
    public void assignFirstLeader(SpreadGroup spreadGroup) {
        this.leader = spreadGroup;
        log.info("Primeiro leader: {}", this.leader);
    }

    @Override
    public void requestWhoIsLeader() {
        log.info("Irei perguntar ao grupo quem é o leader!");
        try{
            connection.multicast(createMulticastMessage(new BaseMessage(BaseMessage.TYPE.WHO_IS_LEADER)));
        } catch (SpreadException exception){
            log.error("Error multicasting message: requestWhoIsLeader ", exception);
        }
    }

    @Override
    public void whoIsLeaderRequestedNotifyParticipantsWhoIsLeader() {
        if(this.amILeader()){
            log.info("LEADER: Alguém perguntou quem é o leader, vou notificar toda a gente que eu sou o leader: {}", this.me);
            this.sendIAmLeaderMessage();
        }
    }

    @Override
    public boolean doIHaveLeader() {
        return !(this.leader == null);
    }

    @Override
    public void appendDataReceived(SpreadMessage spreadMessage) throws SpreadException {
        if(!this.amILeader()){
            NewDataFromLeader appendData = (NewDataFromLeader) spreadMessage.getObject();
            log.info("Informação recebida do leader: {} para ser replicada!", spreadMessage.getSender());
            database.save(DataEntity.builder()
                    .key(appendData.getData().getKey())
                    .data(appendData.getData() == null ? new DataEntity.Data() : appendData.getData().getData() )
                    .build());

            WaitingDataWriteCallback waitingDataWriteCallback = waitingDataWritten.get(appendData.getData().getKey());
            if(waitingDataWriteCallback != null){
                log.info("Resposta recebida para a key: {}", appendData.getData().getKey());
                waitingDataWriteCallback.dataWritten(true);
            }
        }
    }

    @Override
    public void dataRequestedToLeader(SpreadMessage spreadMessage) throws SpreadException {
        if(this.amILeader()){
            AskDataToLeader askDataToLeader = (AskDataToLeader) spreadMessage.getObject();
            log.info("LEADER: {} perguntou-me se tenho informação sobre a key: {}",spreadMessage.getSender(), askDataToLeader.getKey());
            Optional<DataEntity> data = this.database.findById(askDataToLeader.getKey());
            if(data.isPresent()){
                log.info("LEADER: Tenho a informação: {}", data.get());
                connection.multicast(createUnicastMessage(new ResponseDataFromLeader(data.get()), spreadMessage.getSender()));
            }else{
                log.info("LEADER: Não tenho a informação!");
                connection.multicast(createUnicastMessage(new ResponseDataFromLeader(askDataToLeader.getKey()), spreadMessage.getSender()));
            }
        }
    }

    @Override
    public void receivedResponseData(SpreadMessage spreadMessage) throws SpreadException {
        ResponseDataFromLeader data = (ResponseDataFromLeader) spreadMessage.getObject();
        log.info("Received data from leader: " + data);
      //  waitingData.get(data.getData().getKey()).dataReceived(data.getData());
    }

    @Override
    public void writeDataToLeader(SpreadMessage spreadMessage) throws SpreadException {
        if(this.amILeader()){
            WriteDataToLeader data = (WriteDataToLeader) spreadMessage.getObject();
            log.info("LEADER: Recebi informação para ser escrita: {}", data);
            this.saveDataAndUpdateParticipants(new DataEntity(data.getDataEntity()));
        }
    }

    @Override
    public void startupDataRequested(SpreadMessage spreadMessage) {
        if(this.amILeader()){
            log.info("LEADER: {} pediu informação de startup", spreadMessage.getSender());
            /*try{
                connection.multicast(createUnicastMessage(new StartupResponse(this.database.findAll()), spreadMessage.getSender()));
            }catch (SpreadException exception){
                System.out.println("Error multicasting message: requestDataUpdate " + exception);
            }
            */
        }
    }

    @Override
    public void startupDataResponse(SpreadMessage spreadMessage) throws SpreadException {
        if(waitStartupDataUpdate){
            StartupResponse startupResponse = (StartupResponse)spreadMessage.getObject();
           // this.database.saveAll(startupResponse.getDataEntityList());
            log.info(this.database.findById("key").toString());
        }
        log.info("Update data finished!");
    }

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
    public void invalidateData(SpreadMessage spreadMessage) throws SpreadException {
        if(spreadMessage.getSender().equals(this.me)){
            return;
        }
        InvalidateData invalidateData = (InvalidateData) spreadMessage.getObject();
        log.info("Invalidação recebida para a key: {}", invalidateData.getKey());
        if(this.database.findById(invalidateData.getKey()).isPresent()){
            log.info("A remover key: {}", invalidateData.getKey());
            this.database.deleteById(invalidateData.getKey());
        }
    }

    @Override
    public boolean isLeaderMechanism() {
        return this.leaderMechanism;
    }

    @Override
    public void checkStartup(MembershipInfo info) throws SpreadException {
        if(waitStartupDataUpdate){
            log.info("A eleger leader para me ajudar no startup.");
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

            this.leader = info.getMembers()[leaderIndex];
            log.info("Elegi o leader {} com cardinalidade: {}", this.leader, max);

            List<DataEntity.DataDto> dataDtos = new ArrayList<>();
            this.database.findAll().forEach( dataEntity -> dataDtos.add(new DataEntity.DataDto(dataEntity)));

            log.info("Vou pedir startup para a informação: {}", dataDtos);
            connection.multicast(createUnicastMessage(new StartupRequestUpdate(dataDtos), this.leader));

        }
    }

    @Override
    public void sendStartupData(SpreadMessage spreadMessage) throws SpreadException {
        log.info("LEADER=={}: Recebi o pedido de ajuda para o startup de: {}", this.me, spreadMessage.getSender());
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

    public boolean isStartup(){return waitStartupDataUpdate;}
    public boolean amILeader(){
        return this.leader != null && this.leader.equals(this.me);
    }

    public void sendAppendDataToParticipants(DataEntity data){
        if(this.amILeader()){
            log.info("LEADER: A enviar replica de informação para todos os participantes: {}", data);
            try{
                connection.multicast(createMulticastMessage(new NewDataFromLeader(data)));
            } catch (SpreadException spreadException){
                log.error("Error multicasting message: sendAppendDataToParticipants ", spreadException);
            }
        }
    }

    private String getServerName(SpreadGroup group){
        return group.toString().split("#")[1];
    }
    private String getServerIp(SpreadGroup group){
        return group.toString().split("#")[2];
    }

    private int getServerNameCardinality(SpreadGroup group){
        String server = this.getServerName(group);
        String ip = this.getServerIp(group);
        return Integer.parseInt(server.substring(server.length()-1)) + Integer.parseInt(ip.substring(ip.length()-1));
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


    private void sendIAmLeaderMessage() {
        try{
            connection.multicast(createMulticastMessage(new NewLeader()));
        }catch (SpreadException exception){
            log.error("Error multicasting message: sendIAmLeaderMessage ", exception);
        }
    }

    private final Map<String, WaitingDataReadCallback> waitingData = new HashMap<>();

    public DataEntity.DataDto requestDataToLeader(Key request) throws InterruptedException {
        AtomicBoolean received = new AtomicBoolean(false);
        AtomicReference<DataEntity.DataDto> response = new AtomicReference<>();
        WaitingDataReadCallback waitingDataReadCallback = (dataEntity) -> {
            log.info("Pedido de informação ao leader recebido: {}", dataEntity);
            response.set(dataEntity);
            received.set(true);
        };

        try{
            log.info("Vou pedir informação ao leader, key: {}", request.getKey());
            connection.multicast(createUnicastMessage(new AskDataToLeader(request.getKey()), this.leader));
            waitingData.put(request.getKey(), waitingDataReadCallback);
        }catch (SpreadException exception){
            log.error("Error multicasting message: requestDataToLeader ", exception);
        }

        waitForResponse(received);

        waitingData.remove(request.getKey());
        log.info("Resposta recebida");

        return response.get();
    }

    private final Map<String, WaitingDataWriteCallback> waitingDataWritten = new HashMap<>();


    public boolean writeDataToLeader(Data request) throws InterruptedException {
        AtomicBoolean received = new AtomicBoolean(false);
        AtomicReference<Boolean> response = new AtomicReference<>();
        WaitingDataWriteCallback waitingDataWriteCallback = successful -> {
            log.info("Pedido de escrita ao leader completado com estado: {}", successful);
            response.set(successful);
            received.set(true);
        };

        try{
            log.info("Vou pedir ao leader para escrever a informação: {}", request);
            connection.multicast(createUnicastMessage(
                    new WriteDataToLeader(new DataEntity(request.getKey(), new DataEntity.Data(request.getData()))),
                    this.leader));

            waitingDataWritten.put(request.getKey(), waitingDataWriteCallback);
        }catch (SpreadException exception){
            log.error("Error multicasting message: writeDataToLeader ", exception);
        }

        waitForResponse(received);

        waitingDataWritten.remove(request.getKey());

        log.info("Informação {} escrita no leader com estado: {}", request,(response.get() ? "successfully" : "unsuccessfully"));

        return response.get();
    }

    public SpreadGroup getLeader() {
        return leader;
    }

    public void saveDataAndUpdateParticipants(DataEntity request) {
        this.database.save(request);
        log.info("LEADER: Informação guardada na db: {}", request);
        this.sendAppendDataToParticipants(request);
    }

    public DataEntity.DataDto requestData(String key) {
        AtomicReference<DataEntity.DataDto> response = new AtomicReference<>(null);
        final AtomicBoolean responseReceived = new AtomicBoolean(false);

        WaitingDataReadCallback waitingDataReadCallback = (dataDto) -> {
            //System.out.println("Response callback: " + dataDto);
            //System.out.println("Informação pedida recebida para a data: " + key) + ", com informação";
            log.info("Informação pedida recebida para a data: {} com informação: {}", key, dataDto);
            response.set(dataDto);
            responseReceived.set(true);
        };

        try{
            log.info("Vou pedir a todos, se alguém tem: {}", key );
            //System.out.println("Request data: " + key + " to group.");
            connection.multicast(createMulticastMessage(new AskData(key)));

            waitingData.put(key, waitingDataReadCallback);

            waitForResponse(responseReceived);

            waitingData.remove(key);
        }catch (SpreadException | InterruptedException exception){
            log.error("Error multicasting message: requestData ", exception);
        }

        return response.get();
    }


    public void writeData(DataEntity.DataDto dataDto) {
        try{

            Optional<DataEntity> dataEntity = this.database.findById(dataDto.getKey());
            DataEntity update;
            if(dataEntity.isPresent()){
                update = dataEntity.get();
                update.setData(new DataEntity.Data(dataDto.getData()));
                log.info("Eu tenho esta, atualiza localmente.");
            }else{
                log.info("Eu não tenho esta, cria nova localmente.");
                update = new DataEntity(dataDto);
            }

            this.database.save(update);

            log.info("Data armazenada localmente, envia mensagem de invalidação para os restantes participantes.");
            connection.multicast(createMulticastMessage(new InvalidateData(dataDto.getKey())));

        }catch (SpreadException exception){
            log.error("Error multicasting message: writeData ", exception);
        }

    }

    public interface WaitingDataReadCallback {
        void dataReceived(DataEntity.DataDto dataDto);
    }

    public interface WaitingDataWriteCallback {
        void dataWritten(boolean successful);
    }

    private void waitForResponse(final AtomicBoolean responseReceived) throws InterruptedException {
        long start = System.currentTimeMillis();
        long end = start + (long)timeoutInSec * 1000;
        while(!responseReceived.get()) {
            Thread.sleep(100);
            if(System.currentTimeMillis() > end) {
                log.warn("Timeout reached!");
                break;
            }
        }
    }
}
