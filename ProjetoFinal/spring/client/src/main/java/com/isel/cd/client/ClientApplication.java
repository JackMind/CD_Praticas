package com.isel.cd.client;

import io.grpc.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import rpcsclientstubs.ClientServiceGrpc;
import rpcsclientstubs.Data;
import rpcsclientstubs.Key;
import rpcsconfigurationtubs.ConfigurationServiceGrpc;
import rpcsconfigurationtubs.ListServers;
import rpcsconfigurationtubs.Void;

import java.util.*;

@SpringBootApplication
public class ClientApplication implements CommandLineRunner {

    @Value("${serviceIP}")
    String serviceIP;
    @Value("${configurationServiceGrpcPort}")
    int configurationServiceGrpcPort;

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    private static final Map<String, Server> availableServers = new HashMap<>();

    @Override
    public void run(String... args) throws Exception {

        ManagedChannel configurationServiceChannel;
        ManagedChannel serverChannel = null;

        //Criar o meio de comunicação para receber spread servers do Configuration Service
        configurationServiceChannel = ManagedChannelBuilder
                .forAddress(serviceIP, configurationServiceGrpcPort)
                .usePlaintext()
                .build();
        System.out.println("Configuration channel created with ip: " + serviceIP + " on port: " + configurationServiceGrpcPort);

        try{
            ConfigurationServiceGrpc
                    .newStub(configurationServiceChannel)
                    .servers(Void.newBuilder().build(), new ConfigurationServiceObserver(availableServers));
        }catch (Exception exception){
            System.out.println("Configuration Service crashed! ");
            System.exit(1);
        }

        //wait for server updates
        while (availableServers.isEmpty());


        serverChannel = createRandomServerChannel();

        while (true) {
            System.out.println("Start operations!");

            System.out.println("Wait for user input!. [w <key> <value> , r <key>, exit]");
            Scanner myObj = new Scanner(System.in);
            String input = myObj.nextLine();

            String option = input.toLowerCase().split(" ")[0];
            if (serverChannel != null) {
                try {
                    String key;
                    switch (option) {
                        case "r":
                            key = input.split(" ")[1];
                            System.out.println("Read data with key: " + key);
                            Data data = read(key, serverChannel);
                            if(data.getData().isEmpty()){
                                System.out.println("Data not found!");
                            }else{
                                System.out.println("Data: " + data);
                            }
                            break;
                        case "w":
                            key = input.split(" ")[1];
                            String value = input.split(" ")[2];
                            System.out.println("Write data: " + value + " with key: " + key);
                            write(key, value, serverChannel);

                            break;
                        case "exit":
                            System.out.println("Shutting down connections...");
                            configurationServiceChannel.shutdown();
                            serverChannel.shutdown();
                            System.exit(0);
                            break;
                        case "s":
                            serverChannel.shutdownNow();
                            serverChannel = selectServerChannel(input.split(" ")[1], input.split(" ")[2]);
                            break;
                        case "sl":
                            serverChannel.shutdownNow();
                            serverChannel = selectServerChannelFromList(input.split(" ")[1]);
                            break;
                        case "c":
                            String ip = input.split(" ")[1];
                            String port = input.split(" ")[2];
                            serverChannel.shutdownNow();
                            serverChannel = createServerChannel(ip, port);
                            break;
                        case "ls":
                            printServerList();
                            break;
                        default:
                            System.out.println("Option: " + option);
                            System.out.println("Invalid option. [w <key> <value> , r <key>, exit]");
                    }
                }catch (Exception exception) {
                        System.out.println("Exception occurred, try new server! " + exception);
                        serverChannel = createRandomServerChannel();
                }
            } else {
                System.out.println("No channel... try to connect to one.");
                serverChannel.shutdownNow();
                serverChannel = createRandomServerChannel();
            }

        }


    }

    private ManagedChannel selectServerChannelFromList(String s) {
        Server choosedServer = availableServers.get(Integer.parseInt(s) - 1);
        System.out.println("Choose server: " + choosedServer.getName());
        return ManagedChannelBuilder.forAddress(choosedServer.getIp(), choosedServer.getPort()).usePlaintext().build();
    }

    private ManagedChannel createServerChannel(String ip, String port) {
        if(availableServers.isEmpty()){
            System.out.println("No servers available!");
            return null;
        }
        System.out.println("Connecting to Ip: " + ip + " ,port: " + port);
        return ManagedChannelBuilder.forAddress(ip, Integer.parseInt(port)).usePlaintext().build();
    }


    private void printServerList(){
        availableServers.forEach((s, server) -> System.out.println(server));
    }

    private ManagedChannel selectServerChannel(String serverId, String nodeId) {
        if(availableServers.isEmpty()){
            System.out.println("No servers available!");
            return null;
        }
        String select = String.format("#Server%s#spreadNode%s",serverId, nodeId);
        System.out.println("Select: " + select);
        Server choosedServer = availableServers.get(select);
        System.out.println("Choose server: " + choosedServer.getName());
        return ManagedChannelBuilder.forAddress(choosedServer.getIp(), choosedServer.getPort()).usePlaintext().build();
    }


    private ManagedChannel createRandomServerChannel(){
        if(availableServers.isEmpty()){
            System.out.println("No servers available!");
            return null;
        }
        Object[] list = availableServers.keySet().toArray();
        int random = new Random().nextInt(list.length);

        Server choosedServer = availableServers.get((String)list[random]);
        System.out.println("Choose server: " + choosedServer.getName());
        return ManagedChannelBuilder.forAddress(choosedServer.getIp(), choosedServer.getPort()).usePlaintext().build();
    }

    private Data read(String key, Channel channel) throws Exception {
        try{
            return ClientServiceGrpc
                    .newBlockingStub(channel)
                    .read(Key.newBuilder().setKey(key).build());
        }catch (Exception exception){
            System.out.println("Exception occurred on read, try new server! " + exception);
            channel = createRandomServerChannel();
            if(channel == null){
                throw new Exception("No servers available.");
            }
            return read(key, channel);
        }
    }

    private void write(String key, String value, Channel channel) throws Exception {
        try{
            ClientServiceGrpc
                    .newBlockingStub(channel)
                    .write(Data.newBuilder().setData(value).setKey(key).build());
        }catch (Exception exception){
            if(exception instanceof StatusRuntimeException){
                StatusRuntimeException statusRuntimeException = (StatusRuntimeException)exception;
                if(statusRuntimeException.getStatus().equals(Status.DATA_LOSS)){
                    System.out.println("Tenta outra vez.");
                    return;
                }
            }
            System.out.println("Exception occurred on write, try new server! " + exception);
            channel = createRandomServerChannel();
            if(channel == null){
                throw new Exception("No servers available.");
            }
            write(key, value, channel);
        }
    }

}
