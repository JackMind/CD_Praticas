package com.isel.cd.server;

import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;

import java.io.IOException;
import java.net.InetAddress;

@Slf4j
public class SpreadServer implements Runnable{


    private final int port;
    private final int grpcPort;
    private final String groupId;
    private final String hostname;
    private final String name;
    private final boolean local;
    private final boolean leader;
    private final int timeout;
    private final DatabaseRepository database;
    private SpreadConnection connection;

    public SpreadServer(int port, int grpcPort, String groupId, String hostname, String name, boolean local, final DatabaseRepository database, boolean leader, int timeout) {
        this.port = port;
        this.grpcPort = grpcPort;
        this.groupId = groupId;
        this.hostname = hostname;
        this.name = name;
        this.local = local;
        this.database = database;
        this.leader = leader;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        System.out.println();
        System.out.println();
        try
        {

            //Criar Conexaão e Juntar
            connection = new SpreadConnection();
            connection.connect(this.local ? InetAddress.getLocalHost() : InetAddress.getByName(hostname),
                    port, name, false, true);

            LeaderManager leaderManager = new LeaderManager(connection, groupId, database, timeout, leader, local);

            //Listener das mensagens multicast
            SpreadMessageListener msgHandling = new SpreadMessageListener(leaderManager);
            connection.add(msgHandling);

            //Criar Grupo e Juntar
            SpreadGroup group = new SpreadGroup();
            group.join(connection, this.groupId);


            //GRPC Clientes
            io.grpc.Server svc = ServerBuilder
                    .forPort(this.grpcPort)
                    .addService(new ClientService(leaderManager, this.database, this.leader ) )
                    .build();

            svc.start();

        } catch (SpreadException | IOException e) {
            e.printStackTrace();
        }
    }

    public void stop()  {
        try{
            log.info("Stooping server: {}", this.name);
            connection.disconnect();
        }catch (SpreadException spreadException){
            System.out.println(spreadException);
        }
    }

}
