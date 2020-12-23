package com.isel.cd.configurationservice;

import io.grpc.stub.StreamObserver;
import rpcsconfigurationtubs.ListServers;
import rpcsconfigurationtubs.Server;
import spread.SpreadGroup;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientManager implements Runnable{

    private final Map<UUID, StreamObserver<ListServers>> clientsObservers = new ConcurrentHashMap<>();
    private final Deque<ListServers> updateClientsMessages = new ArrayDeque<>();
    private final HashMap<String, Server> spreadServers = new HashMap<>();

    private static final Map<String, Integer> serverPorts = new HashMap<>();
    static{
        serverPorts.put("Server1", 9000);
        serverPorts.put("Server2", 9001);
    }

    public void addSpreadServer(SpreadGroup server){
        if(server.toString().split("#")[1].equals("Service")){
            return;
        }

        String serverName =getServerName(server);
        String serverIp = getServerIp(server);

        Server serverToBeAdded = Server.newBuilder()
            .setName(serverName)
            .setIp(serverIp)
            .setPort(serverPorts.get(serverName))
            .build();

        spreadServers.put(serverName, serverToBeAdded);
        System.out.println("Added server: " + serverToBeAdded);
        this.updateClients();
    }

    public void removeSpreadServer(SpreadGroup server){
        String serverName =getServerName(server);
        spreadServers.remove(serverName);
        System.out.println("Removed server: " + serverName);
        this.updateClients();
    }

    public void printSpreadServers()
    {
        System.out.println();
        System.out.println("Spread Servers:");
        for(Map.Entry<String, Server> member : spreadServers.entrySet())
        {
            System.out.println(member.getKey() + " " + member.getValue());
        }
    }

    public UUID registerClient(StreamObserver<ListServers> clientObserver){
        UUID uuid;
        do{
            uuid = UUID.randomUUID();
        }while (clientsObservers.containsKey(uuid));

        clientsObservers.put(uuid, clientObserver);
        return uuid;
    }

    public void removeClient(UUID clientId){
        clientsObservers.get(clientId).onCompleted();
        clientsObservers.remove(clientId);
    }

    private void updateClients(){
        this.updateClientsMessages.add(getAvailableServers());
        System.out.println("Added message for clients: " + this.updateClientsMessages.size());
    }

    public ListServers getAvailableServers(){
        final ListServers.Builder builder =  ListServers.newBuilder();
        spreadServers.forEach((serverName, server) -> builder.addServers(server));
        return builder.build();
    }

    @Override
    public void run() {
        while (true) {
            if(!this.updateClientsMessages.isEmpty()) {
                System.out.println();
                final ListServers spreadServers = this.updateClientsMessages.pop();

                clientsObservers.forEach((uuid, listServersStreamObserver) -> {
                    System.out.println("Send new spread servers list to " + uuid);
                    listServersStreamObserver.onNext(spreadServers);
                });
            }

            try {
                Thread.sleep(2*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String getServerName(SpreadGroup server) {
        return server.toString().split("#")[1];
    }

    private String getServerIp(SpreadGroup server) {
        return server.toString().split("#")[2];
    }
}
