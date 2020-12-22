package com.company;

import io.grpc.ServerBuilder;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SpreadServer implements Runnable, ClientInterface{


    private final int port;
    private final int grpcPort;
    private final String groupId;
    private final String hostname;
    private final String name;
    private final Database database;

    private SpreadConnection connection;
    public SpreadServer(int port, int grpcPort, String groupId, String hostname, String name) {
        this.port = port;
        this.grpcPort = grpcPort;
        this.groupId = groupId;
        this.hostname = hostname;
        this.name = name;
        this.database = new Database();
    }

    @Override
    public void run() {
        System.out.println();
        System.out.println();
        try
        {

            //Criar Conexaão e Juntar
            connection = new SpreadConnection();
            connection.connect(InetAddress.getByName(hostname), port, name, false, true);

            LeaderManager leaderManager = new LeaderManager(connection, port, grpcPort, groupId, name);

            //Listener das mensagens multicast
            SpreadMessageListener msgHandling = new SpreadMessageListener(leaderManager);
            connection.add(msgHandling);

            //Criar Grupo e Juntar
            SpreadGroup group = new SpreadGroup();
            group.join(connection, this.groupId);


            //GRPC Clientes
            io.grpc.Server svc = ServerBuilder
                    .forPort(this.grpcPort)
                    .addService(new ClientService(leaderManager, this.database) ).build();

            svc.start();


            //Criar Mensagem
            //SpreadMessage message = new SpreadMessage();
            //message.setObject("Ola Mundo");
            //message.addGroup(groupID);     //definir grupo de envio da mensagem
            //message.setReliable();
            //connection.multicast(message); //Enviar Mensagem Multicast

        }
        catch (SpreadException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop()  {
        try{
            System.out.println("Stop server:  " + this.name);
            connection.disconnect();
        }catch (SpreadException spreadException){
            System.out.println(spreadException);
        }
    }

    @Override
    public Database.Data read(String key) {
        //TODO: if not leader redirect to leader

        return this.database.database.get(key);
    }

    @Override
    public void write(String key, Database.Data data) {
        this.database.database.put(key, data);
    }
}
