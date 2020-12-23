package com.isel.cd.configurationservice;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
public class ClientObserver implements StreamObserver<Void>{

    private final ClientManager clientManager;
    public final UUID clientId;


    @Override
    public void onNext(Void value) {
        System.out.println("Received from client! " + value);
    }

    @Override
    public void onError(Throwable t) {
        System.out.println("Error from client! " + t);
    }

    @Override
    public void onCompleted() {
        System.out.println("Completed from client");
        clientManager.removeClient(clientId);
    }
}
