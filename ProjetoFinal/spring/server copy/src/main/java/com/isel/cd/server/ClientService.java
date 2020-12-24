package com.isel.cd.server;

import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import rpcsclientstubs.ClientServiceGrpc;
import rpcsclientstubs.Data;
import rpcsclientstubs.Key;
import rpcsclientstubs.Void;

import java.util.Optional;

public class ClientService extends ClientServiceGrpc.ClientServiceImplBase {

    private final LeaderManager leaderManager;
    private final DatabaseRepository database;

    public ClientService(LeaderManager leaderManager, @Autowired DatabaseRepository database) {
        this.leaderManager = leaderManager;
        this.database = database;
    }

    @Override
    public void read(Key request, io.grpc.stub.StreamObserver<Data> responseObserver) {
        System.out.println("Requested data with key: " + request.getKey());

        String key = request.getKey();
        Optional<DataEntity> data = this.database.findById(key);

        DataEntity response;
        if(data.isEmpty()){
            response = this.leaderManager.requestDataToLeader(request);
        }else {
            response = data.get();
        }

        System.out.println("Sending data... " + request);
        responseObserver.onNext(Data.newBuilder()
                .setData(response.getData() == null ? "" : response.getData() .getData())
                .setKey(response.getKey())
                .build());

        responseObserver.onCompleted();

    }

    @Override
    public void write(Data request, StreamObserver<Void> responseObserver) {
        if(this.leaderManager.amILeader()){
            this.database.save(DataEntity.builder().key(request.getKey()).data(new DataEntity.Data(request.getData())).build());
            //this.database.database.put(request.getKey(), new Database.Data(request.getData()) );
            System.out.println("Saved on leader db" + request);

            responseObserver.onNext(Void.newBuilder().build());
            responseObserver.onCompleted();

            this.leaderManager.sendAppendDataToParticipants(request);
            //CONSENSUS
        }
        /*else{
            System.out.println("Write data to leader: " + this.leaderManager.getLeaderServerName());
            ClientServiceGrpc
                    .newBlockingStub(this.leaderManager.getChannel())
                    .write(Data.newBuilder()
                            .setData(request.getData())
                            .setKey(request.getKey()).build());
            responseObserver.onNext(Void.newBuilder().build());
            responseObserver.onCompleted();
        }*/
    }
}
