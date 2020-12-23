package com.company;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
        if(this.leaderManager.amILeader()){
            System.out.println("here");
            String key = request.getKey();
            Database.Data data = this.database.database.get(key);
            Data dataToCommit = Data.newBuilder()
                                .setData(data.getData())
                                .setKey(request.getKey())
                                .build();

            boolean consensus = this.leaderManager.requestVote(key, data);

            if(consensus){
                System.out.println("Sending data to commit...");
                responseObserver.onNext(dataToCommit);
                responseObserver.onCompleted();
            }else{
                System.out.println("Sending no consensus response.");
                responseObserver.onError(new StatusRuntimeException(Status.UNAVAILABLE));
            }
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
        System.out.println("write");

        if(this.leaderManager.amILeader()){
            this.database.database.put(request.getKey(), new Database.Data(request.getData()) );
            System.out.println("saved on db" + request);

            responseObserver.onNext(Void.newBuilder().build());
            responseObserver.onCompleted();

            this.leaderManager.sendAppendDataToParticipants(request);
            //CONSENSUS
        }else{
            System.out.println("Write data to leader: " + this.leaderManager.getLeaderServerName());
            ClientServiceGrpc
                    .newBlockingStub(this.leaderManager.getChannel())
                    .write(Data.newBuilder()
                            .setData(request.getData())
                            .setKey(request.getKey()).build());
            responseObserver.onNext(Void.newBuilder().build());
            responseObserver.onCompleted();
        }
    }
}
