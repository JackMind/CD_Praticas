import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rpcsclientstubs.Data;
import rpcstubs.*;


public class Cliente
{
    private static String serviceIP = "localhost";
    private static int configurationServiceGrpcPort = 6000;

    private static ManagedChannel configurationServiceChannel;
    private static ManagedChannel channelServer = null;

    public static List<Server> availableServers = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException
    {
        System.out.println("Listening to: " + serviceIP + ":" + configurationServiceGrpcPort + "\n");

        //Criar o meio de comunicação para receber warnings do Service
        configurationServiceChannel = ManagedChannelBuilder.forAddress(serviceIP, configurationServiceGrpcPort).usePlaintext().build();
        ControlServiceGrpc.newStub(configurationServiceChannel).warning(new ConfigurationServiceObserver());



        Thread.sleep(20000);
        System.out.println("Chanel server: " +  channelServer);
        if(channelServer != null)
        {
            DataBaseAccess dataBaseAccess = new DataBaseAccess(channelServer);

            dataBaseAccess.write("key", "bananas");
            Thread.sleep(1000);

            Data data = dataBaseAccess.read("key");
            System.out.println(data);
        }



        //Criar o meio de comunicação para receber warnings
        //ControlServiceGrpc.newStub(channel).warning(new WarningObserver());

       /* ClientServiceGrpc
                .newBlockingStub(channel)
                .write(Data.newBuilder().setData("banas").setKey("key").build());

        Thread.sleep(1000);

        Data data = null;
        try{
            data = ClientServiceGrpc
                    .newBlockingStub(channel)
                    .read(Key.newBuilder().setKey("key").build());
        }catch (Exception exception){
            System.out.println("Exception: " + exception);
        }

        System.out.println(data);

        Scanner myObj = new Scanner(System.in);
        myObj.nextLine();*/
    }

    public static void connectToServer()
    {
        System.out.println("Connect to server!");
        if(availableServers.size() > 0)
        {
            //Atualizar o Channel
            Server choosedServer = availableServers.get(new Random().nextInt(availableServers.size()));
            System.out.println("Choose server: " + choosedServer);
            channelServer = ManagedChannelBuilder.forAddress(choosedServer.getServer_address(), choosedServer.getServer_port()).usePlaintext().build();
            System.out.println("Connecting to " + choosedServer.getServer_address() + "...");
        }
        else
        {
            System.out.println("No Servers Available...");
            channelServer = null;
        }
    }
}
