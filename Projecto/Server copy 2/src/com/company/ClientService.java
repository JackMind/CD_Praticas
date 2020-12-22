package com.company;

import io.grpc.ManagedChannelBuilder;
import rpcsclientstubs.ClientServiceGrpc;
import rpcsclientstubs.Data;
import rpcsclientstubs.Key;
import rpcsclientstubs.Void;

public class ClientService extends ClientServiceGrpc.ClientServiceImplBase {

    private final LeaderManager leaderManager;
    private final Database database;

    public ClientService(LeaderManager leaderManager, Database database) {
        this.leaderManager = leaderManager;
        this.database = database;
    }

    @Override
    public void read(Key request, io.grpc.stub.StreamObserver<Data> responseObserver) {
        if(this.leaderManager.isLeader()){
            Database.Data data = this.database.database.get(request.getKey());
            Data dataToCommit = Data.newBuilder()
                                .setData(data.getData())
                                .setKey(request.getKey())
                                .build();
            responseObserver.onNext(dataToCommit);
            responseObserver.onCompleted();

            //TODO: CONSENSUS
        }else{
            System.out.println("Asking data to leader: " + this.leaderManager.getLeaderServerName());
            Data data = ClientServiceGrpc
                    .newBlockingStub(this.leaderManager.getChannel())
                    .read(request);
            responseObserver.onNext(data);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void write(Data request, io.grpc.stub.StreamObserver<Void> responseObserver) {
        if(this.leaderManager.isLeader()){
            this.database.database.put(request.getKey(), new Database.Data(request.getData()) );
            System.out.println("saved on db" + request);
            responseObserver.onNext(Void.newBuilder().build());
            responseObserver.onCompleted();
            //CONSENSUS
        }else{
            System.out.println("Write data to leader: " + this.leaderManager.getLeaderServerName());
            ClientServiceGrpc
                    .newBlockingStub(this.leaderManager.getChannel())
                    .write(request);
            responseObserver.onCompleted();
        }
    }
}
