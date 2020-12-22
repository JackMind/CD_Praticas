package com.company;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import spread.*;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class LeaderManager implements LeaderManagerInterface{

    private final SpreadConnection connection;
    private final String myIp;
    private final int mySpreadPort;
    private final int myGrpcPort;
    private final String groupId;
    private final String myServerName;


    public LeaderManager(SpreadConnection connection, int mySpreadPort, int myGrpcPort, String groupId, String myServerName) throws UnknownHostException {
        this.connection = connection;
        this.myIp = InetAddress.getLocalHost().getHostAddress();
        this.mySpreadPort = mySpreadPort;
        this.myGrpcPort = myGrpcPort;
        this.groupId = groupId;
        this.myServerName = myServerName;
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
            for(SpreadGroup group1: info.getMembers()){
                String activeServer = getServerName(group1);
                int cardinality = Integer.parseInt(activeServer.substring(activeServer.length()-1));
                if(cardinality > max){
                    max = cardinality;
                }
            }
            this.leaderServerName = "Server"+max;
            System.out.println("New leader: " + this.leaderServerName + " with cardinality: " + max);
        }
        if(isLeader()){
            SpreadMessage message = new SpreadMessage();

            try{
                message.setObject(new NewLeader(this.myIp, this.myGrpcPort, this.leaderServerName));
                message.addGroup(groupId);     //definir grupo de envio da mensagem
                message.setReliable();
                connection.multicast(message); //Enviar Mensagem Multicast
            } catch (SpreadException spreadException){
                System.out.println(spreadException);
            }


        }
    }

    private ManagedChannel channel;
    @Override
    public void selectNewLeader(NewLeader newLeader) {
        if(!newLeader.serverName.equals(this.myServerName)){
            this.leaderServerName = newLeader.serverName;
            this.leaderIp = newLeader.ip;
            this.leaderPort = newLeader.port;
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
    public void firstLeader(SpreadGroup spreadGroup) {
        this.leaderServerName = getServerName(spreadGroup);
        System.out.println("First leader: " + this.leaderServerName);
    }

    @Override
    public void whoIsLeader() {
        SpreadMessage message = new SpreadMessage();

        try{
            message.setObject(new BaseMessage(BaseMessage.TYPE.WHO_IS_LEADER));
            message.addGroup(groupId);     //definir grupo de envio da mensagem
            message.setReliable();
            connection.multicast(message); //Enviar Mensagem Multicast
        } catch (SpreadException spreadException){
            System.out.println(spreadException);
        }

    }

    @Override
    public void notifyParticipants() {
        if(isLeader()){
            System.out.println("Notify that i'm leader: " + this.myServerName);
            SpreadMessage message = new SpreadMessage();

            try{
                message.setObject(new NewLeader(this.myIp, this.myGrpcPort, this.leaderServerName));
                message.addGroup(groupId);     //definir grupo de envio da mensagem
                message.setReliable();
                connection.multicast(message); //Enviar Mensagem Multicast
            } catch (SpreadException spreadException){
                System.out.println(spreadException);
            }
        }
    }

    @Override
    public boolean doIHaveLeader() {
        return !this.leaderServerName.isEmpty();
    }

    public boolean isLeader(){
        //System.out.println(this.leaderServerName + " == " + this.myServerName);
        return this.leaderServerName.equals(this.myServerName);
    }

    public static class NewLeader extends BaseMessage implements Serializable {
        private final String ip;
        private final int port;
        private final String serverName;

        public NewLeader(String ip, int port, String serverName) {
            super(TYPE.NEW_LEADER);
            this.serverName = serverName;
            this.ip = ip;
            this.port = port;
        }

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }

        public String getServerName() {
            return serverName;
        }

        @Override
        public String toString() {
            return "NewLeader{" +
                    "ip='" + ip + '\'' +
                    ", port=" + port +
                    ", serverName='" + serverName + '\'' +
                    '}';
        }
    }


    private String getServerName(SpreadGroup group){
        return group.toString().split("#")[1];
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
}
