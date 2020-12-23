package com.isel.cd.configurationservice;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import rpcsconfigurationtubs.ConfigurationServiceGrpc;
import rpcsconfigurationtubs.ListServers;
import rpcsconfigurationtubs.Void;

import java.util.UUID;

@AllArgsConstructor
public class ConfigurationService extends ConfigurationServiceGrpc.ConfigurationServiceImplBase {
    private final ClientManager clientManager;

    @Override
    public void servers(Void request, StreamObserver<ListServers> responseObserver) {
        UUID clientId = this.clientManager.registerClient(responseObserver);
        new ClientObserver(clientManager, clientId);
        System.out.println("New client registered! " + clientId);
        responseObserver.onNext(this.clientManager.getAvailableServers());
        System.out.println("Available servers sent to client: " + clientId);
    }

}
