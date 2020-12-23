package com.isel.cd.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
public class ServerApplication implements CommandLineRunner {

    @Value("${hostname}")
    private String hostname;
    @Value("${spreadPort}")
    private Integer spreadPort;
    @Value("${grpcPort}")
    private Integer grpcPort;
    @Value("${groupID}")
    private String groupID;
    @Value("${serverName}")
    private String serverName;
    @Value("${local}")
    private Boolean local;
    @Value("${voting}")
    private Boolean voting;
    @Autowired
    private DatabaseRepository database;

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println("Initializing with settings...");
        System.out.println("hostname: " + hostname);
        System.out.println("spreadPort: " + spreadPort);
        System.out.println("grpcPort: " + grpcPort);
        System.out.println("groupID: " + groupID);
        System.out.println("serverName: " + serverName);
        System.out.println("local: " + local);
        System.out.println("voting: " + voting);


        SpreadServer server = new SpreadServer(spreadPort, grpcPort, groupID, hostname, serverName, local, voting, database);
        server.run();


        System.out.println("type exit to exit!");
        Scanner myObj = new Scanner(System.in);
        myObj.nextLine();

        server.stop();
    }
}
