package com.company;

import com.company.messages.AppendData;
import com.company.messages.BaseMessage;
import com.company.messages.NewLeader;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import rpcsclientstubs.Data;
import spread.*;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class LeaderManager implements SpreadMessageListenerInterface {

    private final Database database;
    private final SpreadConnection connection;
    private final String myIp;
    private final int mySpreadPort;
    private final int myGrpcPort;
    private final String groupId;
    private final String myServerName;


    public LeaderManager(SpreadConnection connection,
                         int mySpreadPort,
                         int myGrpcPort,
                         String groupId,
                         String myServerName,
                         final Database database) throws UnknownHostException {
        this.connection = connection;
        this.myIp = InetAddress.getLocalHost().getHostAddress();
        this.mySpreadPort = mySpreadPort;
        this.myGrpcPort = myGrpcPort;
        this.groupId = groupId;
        this.myServerName = myServerName;
        this.database = database;
    }

    private String leaderIp = null;
    private int leaderPort = -1;
    private String leaderServerName = "";

    @Override
    public void notifyServerLeave(SpreadGroup group, MembershipInfo info) {
        String serverName = getServerName(group);
        if(serverName.equals(this.leaderServerName) ){
            System.out.println("Leader: " + this.leaderServerName + " no longer exists");

            int max = 0;
            for(SpreadGroup server: info.getMembers()){
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
                    .forAddress("localhost"/*this.leaderIp*/, 9001/*this.leaderPort*/)
                    .usePlaintext()
                    .build();
            System.out.println("Channel to leader created!");
        }
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
            this.database.database.put(appendData.getKey(), appendData.getData());
        }
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
}
