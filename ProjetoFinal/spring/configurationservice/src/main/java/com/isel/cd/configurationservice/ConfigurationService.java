package com.isel.cd.configurationservice;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rpcsconfigurationtubs.ConfigurationServiceGrpc;
import rpcsconfigurationtubs.ListServers;
import rpcsconfigurationtubs.Void;

import java.util.UUID;

@AllArgsConstructor
@Slf4j
public class ConfigurationService extends ConfigurationServiceGrpc.ConfigurationServiceImplBase {
    private final ClientManager clientManager;

    @Override
    public void servers(Void request, StreamObserver<ListServers> responseObserver) {
        final UUID clientId = this.clientManager.registerClient(responseObserver);
        final ClientObserver clientObserver = new ClientObserver(clientManager, clientId);

        ServerCallStreamObserver<ListServers> serverResponseObserver =
                (ServerCallStreamObserver<ListServers>) responseObserver;

        serverResponseObserver.setOnCancelHandler(clientObserver::onCancel);

        log.info("New client registered! {}", clientId);
        responseObserver.onNext(this.clientManager.getAvailableServers());
        log.info("Available servers sent to client: {}", clientId);
    }

}
