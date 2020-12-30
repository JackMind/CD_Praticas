package com.isel.cd.client;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import rpcsconfigurationtubs.ListServers;

import java.util.Map;

@AllArgsConstructor
public class ConfigurationServiceObserver implements StreamObserver <ListServers> {

    private final Map<String, Server> availableServers;

    @Override
    public void onNext(ListServers value) {
        System.out.println("New Update Received From Configuration Service...");
        System.out.println(value);

        availableServers.clear();
        value.getServersList().forEach(server -> availableServers.put(server.getName(),
                Server.builder().ip(server.getIp()).port(server.getPort()).name(server.getName()).build()));
        System.out.println("Available servers: " + availableServers);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Server crashed " + throwable);
    }

    @Override
    public void onCompleted() {
        System.out.println("Server called on complete");
    }
}
