package com.isel.cd.server;

import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import rpcsclientstubs.ClientServiceGrpc;
import rpcsclientstubs.Data;
import rpcsclientstubs.Key;
import rpcsclientstubs.Void;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
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

    @SneakyThrows
    @Override
    public void read(Key request, io.grpc.stub.StreamObserver<Data> responseObserver) {
        if(leaderManager.isStartup()){
            log.info("Servidor em startup...");
            responseObserver.onNext(Data.newBuilder().build());
            responseObserver.onCompleted();
        }

        log.info("Cliente pediu informação com key: {}",request.getKey());

        String key = request.getKey();
        Optional<DataEntity> data = this.database.findById(key);

        DataEntity.DataDto response;
        if(data.isEmpty()){
            if(withLeader){
                response = this.leaderManager.requestDataToLeader(request);
            }else{
                log.info("Não tenho a key: " + key + ", vou pedir....");
                response = this.leaderManager.requestData(request.getKey());
                //Saved on "local"
                if(response != null){
                    log.info("Outro servidor tinha a data, vou guardar uma replica localmente.");
                    //System.out.println("Saved a local copy of the value");
                    this.database.save(new DataEntity(response));
                }
            }
        } else {
            response = new DataEntity.DataDto(data.get());
        }

        //System.out.println("Sending data... " + response);
        log.info("A enviar resposta para o cliente: {}",response);
        responseObserver
                .onNext(
                    Data.newBuilder()
                    .setData(response == null ? "" : response.getData())
                    .setKey(key)
                    .build());

        responseObserver.onCompleted();
    }

    @SneakyThrows
    @Override
    public void write(Data request, StreamObserver<Void> responseObserver) {
        if(leaderManager.isStartup()){
            log.info("Servidor em startup...");
            responseObserver.onNext(Void.newBuilder().build());
            responseObserver.onCompleted();
        }
        log.info("Um cliente pediu para escrever: {}", request);
        //System.out.println("Write received: " + request);
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
                log.info("Write data to leader: {}", this.leaderManager.getLeader());
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
