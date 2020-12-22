import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

import rpcsclientstubs.ClientServiceGrpc;
import rpcsclientstubs.Data;
import rpcsclientstubs.Key;
import rpcstubs.*;


public class Cliente
{
    static String svcIP="localhost";
    static int svcPort=9000;

    static ManagedChannel channel;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Listening to: " + svcIP + ":" + svcPort);
        System.out.println();

        channel = ManagedChannelBuilder.forAddress(svcIP, svcPort).usePlaintext().build();

        //Criar o meio de comunicação para receber warnings
        //ControlServiceGrpc.newStub(channel).warning(new WarningObserver());

        ClientServiceGrpc
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
        myObj.nextLine();
    }
}
