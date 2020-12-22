package com.company;


import java.util.Scanner;

public class Server1
{

    public static void main(String[] args) throws InterruptedException {
        String hostname = "localhost";
        int spreadPort = 4803;
        int grpcPort = 9000;
        String groupID = "GRUPO123";

        SpreadServer server1 = new SpreadServer(spreadPort, grpcPort, groupID, hostname, "Server1");
        server1.run();


        Scanner myObj = new Scanner(System.in);
        myObj.nextLine();
    }
}
