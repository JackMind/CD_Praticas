package com.isel.cd.server;


import com.isel.cd.server.messages.*;
import org.springframework.scheduling.annotation.Async;
import rpcsclientstubs.Data;
import rpcsclientstubs.Key;
import spread.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
            System.out.println("Leader: " + this.leader + " no longer exists");

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
            System.out.println("New leader: " + this.leader + " with cardinality: " + max);
        }

        if(amILeader()){
            System.out.println("I'm leader, notify everyone!");
            this.sendIAmLeaderMessage();
        }
    }

    @Override
    public void assignNewLeader(SpreadMessage spreadMessage) throws InterruptedException {
        if(!this.me.equals(spreadMessage.getSender())){
            leader = spreadMessage.getSender();
            System.out.println("NEW LEADER! " + leader);

            if(waitStartupDataUpdate){
                this.requestDataUpdate();
            }
        }
    }

    @Async
    void requestDataUpdate() {
        try{
            System.out.println("Request startup data update to leader: " + this.leader);
            connection.multicast(createUnicastMessage(new StartupRequest(), this.leader));

        }catch (SpreadException exception){
            System.out.println("Error multicasting message: requestDataUpdate " + exception);
        }
        waitStartupDataUpdate = false;
    }

    @Override
    public void assignFirstLeader(SpreadGroup spreadGroup) {
        this.leader = spreadGroup;
        System.out.println("First leader: " + this.leader);
    }

    @Override
    public void requestWhoIsLeader() {
        System.out.println("Request who is leader!");
        try{
            connection.multicast(createMulticastMessage(new BaseMessage(BaseMessage.TYPE.WHO_IS_LEADER)));
        } catch (SpreadException exception){
            System.out.println("Error multicasting message: requestWhoIsLeader " + exception);
        }
    }

    @Override
    public void whoIsLeaderRequestedNotifyParticipantsWhoIsLeader() {
        if(this.amILeader()){
            System.out.println("Who is leader received, notify that i am leader! " + this.me);
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
            System.out.println("Data update received from leader!");
            database.save(DataEntity.builder()
                    .key(appendData.getData().getKey())
                    .data(appendData.getData() == null ? new DataEntity.Data() : appendData.getData().getData() )
                    .build());

            WaitingDataWriteCallback waitingDataWriteCallback = waitingDataWritten.get(appendData.getData().getKey());
            if(waitingDataWriteCallback != null){
                System.out.println("Calling response callback...");
                waitingDataWriteCallback.dataWritten(true);
            }
        }
    }

    @Override
    public void dataRequestedToLeader(SpreadMessage spreadMessage) throws SpreadException {
        if(this.amILeader()){
            AskDataToLeader askDataToLeader = (AskDataToLeader) spreadMessage.getObject();
            System.out.println("Data requested to leader: " + askDataToLeader);
            Optional<DataEntity> data = this.database.findById(askDataToLeader.getKey());
            if(data.isPresent()){
                System.out.println("Data found: " + data.get());
                connection.multicast(createUnicastMessage(new ResponseDataFromLeader(data.get()), spreadMessage.getSender()));
            }else{
                System.out.println("Data not found");
                connection.multicast(createUnicastMessage(new ResponseDataFromLeader(askDataToLeader.getKey()), spreadMessage.getSender()));
            }
        }
    }

    @Override
    public void receivedResponseData(SpreadMessage spreadMessage) throws SpreadException {
        ResponseDataFromLeader data = (ResponseDataFromLeader) spreadMessage.getObject();
        System.out.println("Received data from leader: " + data);
        System.out.println("Calling response callback...");
      //  waitingData.get(data.getData().getKey()).dataReceived(data.getData());
    }

    @Override
    public void writeDataToLeader(SpreadMessage spreadMessage) throws SpreadException {
        if(this.amILeader()){
            WriteDataToLeader data = (WriteDataToLeader) spreadMessage.getObject();
            System.out.println("Received data to be written by leader: " + data);
            this.saveDataAndUpdateParticipants(new DataEntity(data.getDataEntity()));
        }
    }

    @Override
    public void startupDataRequested(SpreadMessage spreadMessage) {
        if(this.amILeader()){
            System.out.println("Requested startup data from: " + spreadMessage.getSender());
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
            System.out.println(this.database.findById("key"));
        }
        System.out.println("Update data finished!");
    }

    @Override
    public void askDataResponse(SpreadMessage spreadMessage) throws SpreadException {
        AskData askData = (AskData) spreadMessage.getObject();
        System.out.println("Ask data received for key: " + askData.getKey() );
        Optional<DataEntity> dataEntity = this.database.findById(askData.getKey());
        if (dataEntity.isPresent()) {
            System.out.println("Got data: " + dataEntity.get() + " , sending to: " + spreadMessage.getSender());
            try{
                connection.multicast(createUnicastMessage(new AskDataResponse(dataEntity.get()),spreadMessage.getSender() ));
            }catch (SpreadException exception){
                System.out.println("Error multicasting message: requestDataUpdate " + exception);
            }
        }
    }

    @Override
    public void askDataResponseReceived(SpreadMessage spreadMessage) throws SpreadException {
        AskDataResponse askDataResponse = (AskDataResponse) spreadMessage.getObject();

        WaitingDataReadCallback callback = waitingData.get(askDataResponse.getDataDto().getKey());
        if(callback != null){
            System.out.println("Ask data response received: " + askDataResponse.getDataDto());
            callback.dataReceived(askDataResponse.getDataDto());
        }
    }

    @Override
    public void invalidateData(SpreadMessage spreadMessage) throws SpreadException {
        if(spreadMessage.getSender().equals(this.me)){
            return;
        }
        InvalidateData invalidateData = (InvalidateData) spreadMessage.getObject();
        System.out.println("Invalidate data received for key:" + invalidateData.getKey());
        if(this.database.findById(invalidateData.getKey()).isPresent()){
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
            System.out.println("Startup leader: " + this.leader + " with cardinality: " + max);

            List<DataEntity.DataDto> dataDtos = new ArrayList<>();
            this.database.findAll().forEach( dataEntity -> dataDtos.add(new DataEntity.DataDto(dataEntity)));

            System.out.println("Request startup data: " + dataDtos);
            connection.multicast(createUnicastMessage(new StartupRequestUpdate(dataDtos), this.leader));

        }
    }

    @Override
    public void sendStartupData(SpreadMessage spreadMessage) throws SpreadException {
        System.out.println("Send startup data");
        StartupRequestUpdate startupRequestUpdate = (StartupRequestUpdate) spreadMessage.getObject();

        List<DataEntity.DataDto> startupResponse = new ArrayList<>();
        startupRequestUpdate.getDataDtoList().forEach(dataDto -> {
            Optional<DataEntity> dataEntity = this.database.findById(dataDto.getKey());
            dataEntity.ifPresent(entity -> startupResponse.add(new DataEntity.DataDto(entity)));
        });


        System.out.println("Send startup data: " + startupResponse);
        connection.multicast(createUnicastMessage(new StartupResponseUpdate(startupResponse), spreadMessage.getSender()));
    }

    @Override
    public void startupDataReceived(SpreadMessage spreadMessage) throws SpreadException {
        StartupResponseUpdate startupResponseUpdate = (StartupResponseUpdate) spreadMessage.getObject();
        System.out.println("Startup response received... " + startupResponseUpdate);
        startupResponseUpdate.getDataDtoList().forEach(dataDto -> {
            Optional<DataEntity> dataEntity = this.database.findById(dataDto.getKey());
            dataEntity.ifPresent(entity -> {
                entity.setData(new DataEntity.Data(dataDto.getData()) );
                this.database.save(entity);
            });
        });
        waitStartupDataUpdate = false;
    }

    public boolean amILeader(){
        return this.leader != null && this.leader.equals(this.me);
    }

    public void sendAppendDataToParticipants(DataEntity data){
        if(this.amILeader()){
            System.out.println("Send append data to all participants! " + data);
            try{
                connection.multicast(createMulticastMessage(new NewDataFromLeader(data)));
            } catch (SpreadException spreadException){
                System.out.println(spreadException);
            }
        }
    }

    private String getServerName(SpreadGroup group){
        return group.toString().split("#")[1];
    }

    private int getServerNameCardinality(SpreadGroup group){
        String server = this.getServerName(group);
        return Integer.parseInt(server.substring(server.length()-1));
    }

    private SpreadMessage createMulticastMessage(BaseMessage message){
        SpreadMessage spreadMessage = new SpreadMessage();
        try{
            spreadMessage.setObject(message);
            spreadMessage.addGroup(groupId);
            spreadMessage.setReliable();
        }catch (SpreadException exception){
            System.out.println("Error creating message: " + exception);
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
            System.out.println("Error creating message: " + exception);
        }
        return spreadMessage;
    }


    private void sendIAmLeaderMessage() {
        try{
            connection.multicast(createMulticastMessage(new NewLeader()));
        }catch (SpreadException exception){
            System.out.println("Error multicasting message: sendIAmLeaderMessage " + exception);
        }
    }

    private final Map<String, WaitingDataReadCallback> waitingData = new HashMap<>();

    public DataEntity.DataDto requestDataToLeader(Key request) {
        AtomicReference<DataEntity.DataDto> response = new AtomicReference<>();
        WaitingDataReadCallback waitingDataReadCallback = (dataEntity) -> {
            System.out.println("Response callback: " + dataEntity);
            response.set(dataEntity);
        };

        try{
            System.out.println("Request data to leader with key: " + request.getKey());
            connection.multicast(createUnicastMessage(new AskDataToLeader(request.getKey()), this.leader));
            waitingData.put(request.getKey(), waitingDataReadCallback);
        }catch (SpreadException exception){
            System.out.println("Error multicasting message: sendIAmLeaderMessage " + exception);
        }

        while (response.get()==null);
        //TODO: timeout

        waitingData.remove(request.getKey());
        System.out.println("Response received");

        return response.get();
    }

    private final Map<String, WaitingDataWriteCallback> waitingDataWritten = new HashMap<>();


    public boolean writeDataToLeader(Data request){
        AtomicReference<Boolean> response = new AtomicReference<>();
        WaitingDataWriteCallback waitingDataWriteCallback = successful -> {
            System.out.println("Response callback data written: " + successful);
            response.set(successful);
        };

        try{
            System.out.println("Write data to leader with key: " + request);
            connection.multicast(createUnicastMessage(
                    new WriteDataToLeader(new DataEntity(request.getKey(), new DataEntity.Data(request.getData()))),
                    this.leader));

            waitingDataWritten.put(request.getKey(), waitingDataWriteCallback);
        }catch (SpreadException exception){
            System.out.println("Error multicasting message: writeDataToLeader " + exception);
        }

        while (response.get()==null);
        //TODO: timeout

        waitingDataWritten.remove(request.getKey());
        System.out.println("Data written " + (response.get() ? "successfully" : "unsuccessfully") );

        return response.get();
    }

    public SpreadGroup getLeader() {
        return leader;
    }

    public void saveDataAndUpdateParticipants(DataEntity request) {
        this.database.save(request);
        System.out.println("Saved on leader db" + request);
        this.sendAppendDataToParticipants(request);
    }

    public DataEntity.DataDto requestData(String key) {
        AtomicReference<DataEntity.DataDto> response = new AtomicReference<>(null);
        final AtomicBoolean responseReceived = new AtomicBoolean(false);

        WaitingDataReadCallback waitingDataReadCallback = (dataDto) -> {
            System.out.println("Response callback: " + dataDto);
            response.set(dataDto);
            responseReceived.set(true);
        };

        try{
            System.out.println("Request data: " + key + " to group.");
            connection.multicast(createMulticastMessage(new AskData(key)));

            waitingData.put(key, waitingDataReadCallback);

            waitForResponse(responseReceived);

            waitingData.remove(key);
        }catch (SpreadException exception){
            System.out.println("Error multicasting message: writeDataToLeader " + exception);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
            }else{
                update = new DataEntity(dataDto);
            }

            this.database.save(update);

            System.out.println("Send invalidate data: " + dataDto + " to group.");
            connection.multicast(createMulticastMessage(new InvalidateData(dataDto.getKey())));

        }catch (SpreadException exception){
            System.out.println("Error multicasting message: writeDataToLeader " + exception);
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
                System.out.println("Timeout reached!");
                break;
            }
        }
    }
}
