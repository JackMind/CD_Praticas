package com.isel.cd.server;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
        if(this.leaderManager.amILeader()){
            String key = request.getKey();
            Optional<DataEntity> data = this.database.findById(request.getKey());

            if(data.isEmpty()){
                System.out.println("DO SOMEHTING");
                //TODO: if data nulll do somethin
                return;
            }


            boolean consensus = this.leaderManager.requestVote(key, data.get());

            if(consensus){
                System.out.println("Sending data...");
                responseObserver.onNext(Data.newBuilder()
                        .setData(data.get().getData().getData())
                        .setKey(data.get().getKey())
                        .build());
                responseObserver.onCompleted();
            }else{
                responseObserver.onError(new StatusRuntimeException(Status.UNAVAILABLE));
            }
        }else{
            System.out.println("Asking data to leader: " + this.leaderManager.getLeaderServerName());
            try {
                Data data = ClientServiceGrpc
                        .newBlockingStub(this.leaderManager.getChannel())
                        .read(request);

                System.out.println("Data received from leader: " + data);
                responseObserver.onNext(data);
                responseObserver.onCompleted();
            }catch (Exception exception){
                System.out.println("received exception: " + exception);
                responseObserver.onError(exception);
            }
        }
    }

    @Override
    public void write(Data request, StreamObserver<Void> responseObserver) {
        if(this.leaderManager.amILeader()){
            this.database.save(DataEntity.builder().key(request.getKey()).data(new DataEntity.Data(request.getData())).build());
            //this.database.database.put(request.getKey(), new Database.Data(request.getData()) );
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
