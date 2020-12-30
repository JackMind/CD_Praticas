package com.isel.cd.configurationservice;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import rpcsconfigurationtubs.ListServers;
import rpcsconfigurationtubs.Server;
import spread.SpreadGroup;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ClientManager implements Runnable{

    private final Map<String, Integer> serverPorts;
    private final Map<String, String> groupIps;
    private final Map<UUID, StreamObserver<ListServers>> clientsObservers = new ConcurrentHashMap<>();
    private final Deque<ListServers> updateClientsMessages = new ArrayDeque<>();
    private final HashMap<String, Server> spreadServers = new HashMap<>();

    public ClientManager(Map<String, Integer> knownServers, Map<String, String> knownGroups) {
        this.serverPorts = knownServers;
        this.groupIps = knownGroups;
    }

    public void addSpreadServer(SpreadGroup server){
        if(server.toString().split("#")[1].equals("Service")){
            return;
        }

        String serverName =getServerName(server);
        String serverIp = getServerIp(server);

        Server serverToBeAdded = Server.newBuilder()
            .setName(server.toString())
            .setIp(groupIps.get(serverIp))
            .setPort(serverPorts.get(serverName))
            .build();

        spreadServers.put(server.toString(), serverToBeAdded);
        log.info("Added server: {}", serverToBeAdded);
        this.updateClients();
    }

    public void removeSpreadServer(SpreadGroup server){
        //String serverName =getServerName(server);
        spreadServers.remove(server.toString());
        log.info("Removed server: {}", server.toString());
        this.updateClients();
    }

    public void printSpreadServers() {
        log.info("Spread Servers:");
        for(Map.Entry<String, Server> member : spreadServers.entrySet())
        {
            log.info("{} {}",member.getKey() , member.getValue());
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
        this.updateClientsMessages.push(getAvailableServers());
        log.info("Added message for clients: {}", this.updateClientsMessages.size());
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
                final ListServers spreadServers = this.updateClientsMessages.pop();
                log.info("Sending new list of servers: {}", spreadServers);
                clientsObservers.forEach((uuid, listServersStreamObserver) -> {
                    log.info("Send new spread servers list to {}", uuid);
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
