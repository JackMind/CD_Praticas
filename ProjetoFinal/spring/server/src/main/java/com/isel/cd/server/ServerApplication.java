package com.isel.cd.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
public class ServerApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String hostname = "localhost";
        int spreadPort = 4803;
        int grpcPort = 9000;
        String groupID = "GRUPO123";
        String serverName = "Server1";
        boolean local = true;

        SpreadServer server = new SpreadServer(spreadPort, grpcPort, groupID, hostname, serverName, local);
        server.run();


        System.out.println("exit to exit!");
        Scanner myObj = new Scanner(System.in);
        myObj.nextLine();

        server.stop();
    }
}
