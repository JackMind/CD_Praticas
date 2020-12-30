package com.isel.cd.configurationservice;

import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import spread.SpreadConnection;
import spread.SpreadGroup;

import java.net.InetAddress;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
@Slf4j
public class ConfigurationserviceApplication implements CommandLineRunner {

    @Value("#{${knowngroups}}")
    private Map<String, String> knownGroups;
    @Value("#{${knownservers}}")
    private Map<String, Integer> knownServers;
    @Value("${grpcServerPort}")
    private Integer grpcServerPort;
    @Value("${SpreadServerPort}")
    private Integer SpreadServerPort;
    @Value("${SpreadGroupName}")
    private String SpreadGroupName;
    @Value("${hostname}")
    private String hostname;
    @Value("${local}")
    private Boolean local;

    public static void main(String[] args) {
        SpringApplication.run(ConfigurationserviceApplication.class, args);
    }

    @Override
    public void run(String... args) {

        log.info("Initializing with settings...");
        log.info("hostname: {}", hostname);
        log.info("spreadPort: {}", SpreadServerPort);
        log.info("grpcPort: {}", grpcServerPort);
        log.info("local: {}", local);
        log.info("knownServers: {}", knownServers);
        log.info("knownGroups: {}", knownGroups);

        try {

            ClientManager clientManager = new ClientManager(knownServers, knownGroups);

            //GRPC C
            io.grpc.Server svc = ServerBuilder.forPort(grpcServerPort).addService(new ConfigurationService(clientManager)).build();
            svc.start();
            log.info("Grpc Server started, listening on " + grpcServerPort);

            new Thread(clientManager).start();


            //SPREAD Servidores
            SpreadConnection connection = new SpreadConnection();
            connection.connect(local ? InetAddress.getLocalHost() : InetAddress.getByName(hostname),
                    SpreadServerPort, "Service", false, true);

            connection.add(new MessageListener(clientManager));

            SpreadGroup group = new SpreadGroup();
            group.join(connection, SpreadGroupName);


            Scanner myObj = new Scanner(System.in);
            myObj.nextLine();



            connection.disconnect();
            svc.shutdown();
        }
        catch(Exception ex) {
            log.error("!! Exceção !!", ex);
        }
    }
}
