package com.isel.cd.server;

import io.grpc.Status;
import io.grpc.StatusException;
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

    public ClientService(LeaderManager leaderManager, @Autowired DatabaseRepository database) {
        this.leaderManager = leaderManager;
        this.database = database;
    }

    @SneakyThrows
    @Override
    public void read(Key request, io.grpc.stub.StreamObserver<Data> responseObserver) {
        try{
            if(leaderManager.isServerStartingUp()){
                log.info("Servidor em startup...");
                responseObserver.onNext(Data.newBuilder().build());
                responseObserver.onCompleted();
            }

            log.info("Cliente pediu informacao com [{}]",request.getKey());

            String key = request.getKey();
            Optional<DataEntity> data = this.database.findById(key);

            DataEntity.DataDto response;
            if(data.isEmpty()){
                log.info("Não tenho a [{}], vou pedir...", key);
                response = this.leaderManager.requestData(request.getKey());
            } else {
                DataEntity local = data.get();
                if(local.getInvalidate() != null && local.getInvalidate().equals(Boolean.TRUE)){
                    log.info("Tenho replica local mas esta invalida [{}] invalida", key);
                    response = null;
                }else{
                    response = new DataEntity.DataDto(data.get());
                    log.info("Tenho replica local {}", response);
                }
            }

            log.info("A enviar resposta para o cliente: {}",response);
            responseObserver
                    .onNext(
                        Data.newBuilder()
                        .setData(response == null ? "" : response.getData())
                        .setKey(key)
                        .build());

            responseObserver.onCompleted();
        }catch (IllegalStateException exception){ }
    }

    @SneakyThrows
    @Override
    public void write(Data request, StreamObserver<Void> responseObserver) {
        try{
            if(leaderManager.isServerStartingUp()){
                log.info("Servidor em startup...");
                responseObserver.onNext(Void.newBuilder().build());
                responseObserver.onCompleted();
            }
            log.info("Um cliente pediu para escrever: {}", request);

            DataEntity.DataDto dataDto = new DataEntity.DataDto(request.getKey(), request.getData());
            boolean success = this.leaderManager.writeData(dataDto);

            if(success){
                responseObserver.onNext(Void.newBuilder().build());
                responseObserver.onCompleted();
            }else {
                responseObserver.onError(new StatusException(Status.DATA_LOSS));
                responseObserver.onCompleted();
            }
        }catch (IllegalStateException exception){ }

    }
}
