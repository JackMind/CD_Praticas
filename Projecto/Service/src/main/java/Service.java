import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import rpcstubs.WarnMsg;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class Service
{
    private final static int grpcServerPort = 6000;
    public static MessageBroadCast messageBroadCast;

    private final static int SpreadServerPort = 4803;
    private final static String SpreadGroupName = "GRUPO123";
    public static ObservableHashMap<String, SpreadGroup> groupMembers = new ObservableHashMap<String,SpreadGroup>();

    public static void main(String[] args)
    {
        System.out.println();
        System.out.println();

        try
        {
            //GRPC Clientes
            io.grpc.Server svc = ServerBuilder.forPort(grpcServerPort).addService(new ControlService()).build();
            svc.start();
            System.out.println("Grpc Server started, listening on " + grpcServerPort);
            System.out.println();

            messageBroadCast = new MessageBroadCast();
            new Thread(messageBroadCast).start();


            //SPREAD Servidores
            SpreadConnection connection = new SpreadConnection();
            //connection.connect(InetAddress.getByName(host), port, "privatename", false, false);
            connection.connect(InetAddress.getLocalHost(), SpreadServerPort, "Service", false, true);

            MessageListener msgHandling =new MessageListener(connection);
            connection.add(msgHandling);

            SpreadGroup group = new SpreadGroup();
            group.join(connection, SpreadGroupName);



            Scanner myObj = new Scanner(System.in);
            myObj.nextLine();



            connection.disconnect();
            svc.shutdown();
        }
        catch(Exception ex)
        {
            System.out.println("!! Exceção !!");
            ex.printStackTrace();
        }
    }

    public static void imprimirMembros()
    {
        System.out.println();
        System.out.println("Membros:");
        for(Map.Entry<String, SpreadGroup> member : groupMembers.entrySet())
        {
            System.out.println(member.getKey());
        }
    }

    public static String serverList()
    {
        StringBuilder str = new StringBuilder();
        str.append("Active Servers:\n");
        for(Map.Entry<String, SpreadGroup> entry : groupMembers.entrySet())
        {
            str.append("- ").append(entry.getKey()).append("\n");
        }

        return str.toString();
    }
}
