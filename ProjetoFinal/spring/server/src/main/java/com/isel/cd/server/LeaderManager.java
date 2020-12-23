package com.isel.cd.server;


import com.isel.cd.server.messages.AppendData;
import com.isel.cd.server.messages.BaseMessage;
import com.isel.cd.server.messages.ConsensusVoting;
import com.isel.cd.server.messages.NewLeader;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import rpcsclientstubs.Data;
import rpcsconsensusstubs.ConsensusServiceGrpc;
import rpcsconsensusstubs.TransactionVote;
import rpcsconsensusstubs.Void;
import spread.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class LeaderManager extends ConsensusServiceGrpc.ConsensusServiceImplBase implements SpreadMessageListenerInterface {

    private final DatabaseRepository database;
    private final SpreadConnection connection;
    private final String myIp;
    private final int myGrpcPort;
    private final String groupId;
    private final String myServerName;
    private final ConsensusModule consensusModule;
    private final boolean readConsensus;

    public LeaderManager(SpreadConnection connection,
                         int myGrpcPort,
                         String groupId,
                         String myServerName,
                         final DatabaseRepository database,
                         final boolean readConsensus) throws UnknownHostException {
        this.connection = connection;
        this.myIp = InetAddress.getLocalHost().getHostAddress();
        this.myGrpcPort = myGrpcPort;
        this.groupId = groupId;
        this.myServerName = myServerName;
        this.database = database;
        this.consensusModule = new ConsensusModule();
        this.readConsensus = readConsensus;
    }

    private String leaderIp = null;
    private int leaderPort = -1;
    private String leaderServerName = "";
    private volatile int spreadMembersSize;
    private volatile boolean waitStartupDataUpdate = true;

    @Override
    public void notifyServerLeave(SpreadGroup group, MembershipInfo info) {
        String serverName = getServerName(group);
        if(serverName.equals(this.leaderServerName) ){
            System.out.println("Leader: " + this.leaderServerName + " no longer exists");

            int max = 0;
            for(SpreadGroup server: info.getMembers()){
                if(this.getServerName(server).equals("Service")){
                    continue;
                }
                int cardinality = this.getServerNameCardinality(server);
                if(cardinality > max){
                    max = cardinality;
                }
            }
            this.leaderServerName = "Server"+max;
            System.out.println("New leader: " + this.leaderServerName + " with cardinality: " + max);
        }

        if(amILeader()){
            System.out.println("I'm leader, notify everyone!");
            this.sendIAmLeaderMessage();
        }
    }

    private ManagedChannel channel;
    @Override
    public void assignNewLeader(NewLeader newLeader) {
        if(!newLeader.getServerName().equals(this.myServerName)){
            this.leaderServerName = newLeader.getServerName();
            this.leaderIp = newLeader.getIp();
            this.leaderPort = newLeader.getPort();
            System.out.println("NEW LEADER! " + newLeader);

            this.channel = ManagedChannelBuilder
                    .forAddress(this.leaderIp, this.leaderPort)
                    .usePlaintext()
                    .build();
            System.out.println("Channel to leader created!");

            if(waitStartupDataUpdate){
                this.requestDataUpdate();
            }
        }
    }

    private void requestDataUpdate() {
        AtomicBoolean workingOnUpdate = new AtomicBoolean(true);
        StreamObserver<rpcsconsensusstubs.Data> dataStreamObserver = new StreamObserver<>() {
            @Override
            public void onNext(rpcsconsensusstubs.Data value) {
                System.out.println("Data updated: " + value);
                database.save(DataEntity.builder().key(value.getKey()).data(new DataEntity.Data(value.getData())).build());
                //database.database.put(value.getKey(), new Database.Data(value.getData()));
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Error updating data: " + t);
                workingOnUpdate.set(false);
            }

            @Override
            public void onCompleted() {
                workingOnUpdate.set(false);
            }
        };

        ConsensusServiceGrpc
                .newStub(this.channel)
                .update(Void.newBuilder().build(), dataStreamObserver);


        while (workingOnUpdate.get());

        waitStartupDataUpdate = false;
        System.out.println("Update data finished!");
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    @Override
    public void assignFirstLeader(SpreadGroup spreadGroup) {
        this.leaderServerName = getServerName(spreadGroup);
        System.out.println("First leader: " + this.leaderServerName);
    }

    @Override
    public void requestWhoIsLeader() {
        System.out.println("Request who is leader!");
        try{
            connection.multicast(createMessage(new BaseMessage(BaseMessage.TYPE.WHO_IS_LEADER)));
        } catch (SpreadException exception){
            System.out.println("Error multicasting message: requestWhoIsLeader " + exception);
        }
    }

    @Override
    public void whoIsLeaderRequestedNotifyParticipantsWhoIsLeader() {
        if(this.amILeader()){
            System.out.println("Who is leader received, notify that i am leader! " + this.myServerName);
            this.sendIAmLeaderMessage();
        }
    }

    @Override
    public boolean doIHaveLeader() {
        return !this.leaderServerName.isEmpty();
    }

    @Override
    public void appendDataReceived(AppendData appendData) {
        if(!this.amILeader()){
            System.out.println("Data update received from leader!");
            database.save(DataEntity.builder().key(appendData.getKey()).data(new DataEntity.Data(appendData.getData().getData())).build());
            database.findAll().forEach(System.out::println);
        }
    }

    @Override
    public void updateMembersSize(int length) {
        this.spreadMembersSize = length;
    }

    @Override
    public void handleVoteRequest(ConsensusVoting voting) {
        if(this.amILeader()){
            return;
        }
        System.out.println("Request for voting received: " + voting);
        UUID transactionId = voting.getTransactionId();
        DataEntity.Data data = voting.getData();

        boolean vote = true;

        Optional<DataEntity> localData = this.database.findById(voting.getKey());
        if(localData.isEmpty()){
            vote = false;
        }
        if(localData.get().hashCode()!=data.getData().hashCode()){
            vote = false;
        }

        ConsensusServiceGrpc
                .newBlockingStub(this.channel)
                .vote(TransactionVote.newBuilder()
                        .setTransactionId(transactionId.toString())
                        .setVote(vote)
                        .setServerName(this.myServerName)
                        .build());

        System.out.println(this.myServerName + " voted: " + vote + " to transaction: " +transactionId);
    }

    public boolean amILeader(){
        //System.out.println(this.leaderServerName + " == " + this.myServerName);
        return this.leaderServerName.equals(this.myServerName);
    }

    public void sendAppendDataToParticipants(Data data){
        if(this.amILeader()){
            System.out.println("Send append data to all participants! " + data);
            try{
                connection.multicast(createMessage(new AppendData(data)));
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

    private String getHostName(SpreadGroup group){
        return group.toString().split("#")[2];
    }

    public String getLeaderIp() {
        return leaderIp;
    }

    public int getLeaderPort() {
        return leaderPort;
    }

    public String getLeaderServerName() {
        return leaderServerName;
    }

    private SpreadMessage createMessage(BaseMessage message){
        SpreadMessage spreadMessage = new SpreadMessage();
        try{
            spreadMessage.setObject(message);
            spreadMessage.addGroup(groupId);     //definir grupo de envio da mensagem
            spreadMessage.setReliable();
        }catch (SpreadException exception){
            System.out.println("Error creating message: " + exception);
        }
        return spreadMessage;
    }

    private void sendIAmLeaderMessage() {
        //System.out.println("Notify that i'm leader: " + this.myServerName);
        try{
            connection.multicast(createMessage(new NewLeader(this.myIp, this.myGrpcPort, this.leaderServerName))); //Enviar Mensagem Multicast
        }catch (SpreadException exception){
            System.out.println("Error multicasting message: sendIAmLeaderMessage " + exception);
        }
    }

    public boolean requestVote(String key, DataEntity dataToCommit) {
        if(this.spreadMembersSize==2 || !this.readConsensus){
            return true;
        }
        if(this.amILeader()){
            System.out.println("Submitting vote for data: " + dataToCommit);

            AtomicBoolean completed = new AtomicBoolean(false);

            ConsensusModule.VoteCompletedCallBack callBack = completed::set;

            UUID transactionId = this.consensusModule.submitVote(this.spreadMembersSize, callBack);
            System.out.println("Request voting for transaction: " + transactionId);
            try{
                connection.multicast(createMessage(new ConsensusVoting(transactionId, key, dataToCommit.getData())));
            } catch (SpreadException spreadException){
                System.out.println(spreadException);
            }

            while (!completed.get()){ }

            boolean consensus = this.consensusModule.getResultAndRemove(transactionId);
            System.out.println("Consensus completed with value: " + consensus);
            return consensus;
        }
        return false;
    }

    @Override
    public void vote(TransactionVote request, StreamObserver<Void> responseObserver) {
        if(this.amILeader()){
            System.out.println("Vote received from: " + request.getServerName()
                    + " to transaction: " + request.getTransactionId()
                    + " with vote: " + request.getVote());

            this.consensusModule.appendVote(UUID.fromString(request.getTransactionId()), request.getVote());
        }
        responseObserver.onNext(Void.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void update(Void request, StreamObserver<rpcsconsensusstubs.Data> responseObserver) {
        if(this.amILeader()){
            System.out.println("Participant requested data update!");
            this.database.findAll().forEach(dataEntity ->
                    responseObserver.onNext(
                            rpcsconsensusstubs.Data.newBuilder()
                                    .setKey(dataEntity.getKey())
                                    .setData(dataEntity.getData().getData())
                                    .build()));
            responseObserver.onCompleted();
            System.out.println("Data update finished!");
        }
    }
}
