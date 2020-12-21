import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;
import rpcstubs.*;


public class Cliente
{
    static String svcIP="localhost";
    static int svcPort=6000;

    static ManagedChannel channel;

    public static void main(String[] args)
    {
        System.out.println("Listening to: " + svcIP + ":" + svcPort);
        System.out.println();

        channel = ManagedChannelBuilder.forAddress(svcIP, svcPort).usePlaintext().build();

        //Criar o meio de comunicação para receber warnings
        ControlServiceGrpc.newStub(channel).warning(new WarningObserver());

        Scanner myObj = new Scanner(System.in);
        myObj.nextLine();
    }
}
