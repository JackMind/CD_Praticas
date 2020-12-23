package com.isel.cd.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import rpcsclientstubs.ClientServiceGrpc;
import rpcsclientstubs.Data;
import rpcsclientstubs.Key;
import rpcsconfigurationtubs.ConfigurationServiceGrpc;
import rpcsconfigurationtubs.ListServers;
import rpcsconfigurationtubs.Void;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

@SpringBootApplication
public class ClientApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    private static final List<Server> availableServers = new ArrayList<>();

    @Override
    public void run(String... args) throws Exception {

        String serviceIP = "localhost";
        int configurationServiceGrpcPort = 6000;

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

            System.out.println("Wait for user input!. [w <key> <value> , r <key>]");
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
                            System.out.println("Data: " + data);
                            break;
                        case "w":
                            key = input.split(" ")[1];
                            String value = input.split(" ")[2];
                            System.out.println("Write data: " + value + " with key: " + key);
                            write(key, value, serverChannel);
                            break;
                        default:
                            System.out.println("Option: " + option);
                            System.out.println("Invalid option. [w <key> <value> , r <key>]");
                    }
                }catch (Exception exception) {
                        System.out.println("Exception occurred, try new server! " + exception);
                        serverChannel = createRandomServerChannel();
                }
            } else {
                System.out.println("No channel... try to connect to one.");
                serverChannel = createRandomServerChannel();
            }

        }


    }


    private ManagedChannel createRandomServerChannel(){
        if(availableServers.isEmpty()){
            System.out.println("No servers available!");
            return null;
        }
        Server choosedServer = availableServers.get(new Random().nextInt(availableServers.size()));
        System.out.println("Choose server: " + choosedServer.getName());
        return ManagedChannelBuilder.forAddress(choosedServer.getIp(), choosedServer.getPort()).usePlaintext().build();
    }

    private Data read(String key, Channel channel) {
        return ClientServiceGrpc
                .newBlockingStub(channel)
                .read(Key.newBuilder().setKey(key).build());
    }

    private void write(String key, String value, Channel channel) {
        ClientServiceGrpc
                .newBlockingStub(channel)
                .write(Data.newBuilder().setData(value).setKey(key).build());
    }

}
