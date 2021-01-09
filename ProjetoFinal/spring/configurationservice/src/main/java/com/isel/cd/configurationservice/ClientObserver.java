package com.isel.cd.configurationservice;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@AllArgsConstructor
@Slf4j
public class ClientObserver implements StreamObserver<Void>{

    private final ClientManager clientManager;
    public final UUID clientId;


    @Override
    public void onNext(Void value) {
        log.info("Received from client! {}",value);
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error from client! ", t);
    }

    @Override
    public void onCompleted() {
        log.info("Completed from client");
        clientManager.removeClient(clientId);
    }

    public void onCancel(){
        log.info("Client {} left without onCompleted", clientId);
        clientManager.removeClient(clientId);
    }
}
