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

    /*
    @Value("${knowngroups}")
    private Map<String, String> knownGroups;
    @Value("${knownservers}")
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


     */
    @Autowired
    private Configs configs;
    public static void main(String[] args) {
        SpringApplication.run(ConfigurationserviceApplication.class, args);
    }

    @Override
    public void run(String... args) {

        log.info("Initializing with settings...");
        log.info("hostname: {}", configs.getHostname());
        log.info("spreadPort: {}", configs.getSpreadServerPort());
        log.info("grpcPort: {}", configs.getGrpcServerPort());
        log.info("local: {}", configs.getLocal());
        log.info("knownServers: {}", configs.getKnownServers());
        log.info("knownGroups: {}", configs.getKnownGroups());

        try {

            ClientManager clientManager = new ClientManager(configs.getKnownServers(), configs.getKnownGroups());

            //GRPC C
            io.grpc.Server svc = ServerBuilder
                    .forPort(configs.getGrpcServerPort())
                    .addService(new ConfigurationService(clientManager))
                    .build();
            svc.start();
            log.info("Grpc Server started, listening on " + configs.getGrpcServerPort());

            new Thread(clientManager).start();


            //SPREAD Servidores
            SpreadConnection connection = new SpreadConnection();
            connection.connect(configs.getLocal() ? InetAddress.getLocalHost() : InetAddress.getByName(configs.getHostname()),
                    configs.getSpreadServerPort(), "Service", false, true);

            connection.add(new MessageListener(clientManager));

            SpreadGroup group = new SpreadGroup();
            group.join(connection, configs.getSpreadGroupName());


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
