package com.isel.cd.server;

import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import rpcsclientstubs.ClientServiceGrpc;
import rpcsclientstubs.Data;
import rpcsclientstubs.Key;
import rpcsclientstubs.Void;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ClientService extends ClientServiceGrpc.ClientServiceImplBase {

    private final Map<String, DataEntity.DataDto> localRepo = new HashMap<>();

    private final LeaderManager leaderManager;
    private final DatabaseRepository database;
    private final boolean withLeader;
    public ClientService(LeaderManager leaderManager, @Autowired DatabaseRepository database, final boolean withLeader) {
        this.leaderManager = leaderManager;
        this.database = database;
        this.withLeader = withLeader;
    }

    @Override
    public void read(Key request, io.grpc.stub.StreamObserver<Data> responseObserver) {
        System.out.println("Requested data with key: " + request.getKey());

        String key = request.getKey();
        Optional<DataEntity> data = this.database.findById(key);

        DataEntity.DataDto response;
        if(data.isEmpty()){
            if(withLeader){
                response = this.leaderManager.requestDataToLeader(request);
            }else{
                response = this.leaderManager.requestData(request.getKey());
                //Saved on "local"
                System.out.println("Saved a local copy of the value");
                this.database.save(new DataEntity(response));
            }
        } else {
            response = new DataEntity.DataDto(data.get());
        }

        System.out.println("Sending data... " + response);
        responseObserver
                .onNext(
                    Data.newBuilder()
                    .setData(response == null ? "" : response.getData())
                    .setKey(request.getKey())
                    .build());

        responseObserver.onCompleted();
    }

    @Override
    public void write(Data request, StreamObserver<Void> responseObserver) {
        System.out.println("Write received: " + request);
        if(withLeader){
            if(this.leaderManager.amILeader()){
                DataEntity dataEntity = DataEntity.builder()
                        .key(request.getKey())
                        .data(new DataEntity.Data(request.getData()))
                        .build();

                this.leaderManager.saveDataAndUpdateParticipants(dataEntity);

                responseObserver.onNext(Void.newBuilder().build());
                responseObserver.onCompleted();
            }else{
                System.out.println("Write data to leader: " + this.leaderManager.getLeader());
                this.leaderManager.writeDataToLeader(request);

                responseObserver.onNext(Void.newBuilder().build());
                responseObserver.onCompleted();
            }
        }else{

            DataEntity.DataDto dataDto = new DataEntity.DataDto(request.getKey(), request.getData());
            this.leaderManager.writeData(dataDto);

            responseObserver.onNext(Void.newBuilder().build());
            responseObserver.onCompleted();
        }

    }
}
