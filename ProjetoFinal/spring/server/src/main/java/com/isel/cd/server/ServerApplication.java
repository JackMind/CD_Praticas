package com.isel.cd.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
@Slf4j
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
    @Value("${leader}")
    private Boolean leader;
    @Value("${timeout}")
    private Integer timeout;
    @Autowired
    private DatabaseRepository database;

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    @Override
    public void run(String... args) {

        log.info("Initializing with settings...");
        log.info("hostname: {}", hostname);
        log.info("spreadPort: {}", spreadPort);
        log.info("grpcPort: {}", grpcPort);
        log.info("groupID: {}", groupID);
        log.info("serverName: {}", serverName);
        log.info("local: {}", local);
        log.info("leader: {}", leader);
        log.info("timeoutInSec: {}", timeout);



        SpreadServer server = new SpreadServer(spreadPort, grpcPort, groupID, hostname, serverName, local, database, leader, timeout);
        server.run();


        log.info("type exit to exit!");
        Scanner myObj = new Scanner(System.in);
        myObj.nextLine();

        server.stop();
    }
}
