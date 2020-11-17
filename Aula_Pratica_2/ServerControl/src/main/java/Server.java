import centralstubs.CentralServiceGrpc;
import centralstubs.Tariff;
import centralstubs.Track;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import rpcstubs.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Server {
    private static Map<String, Initial> veiculosEstrada;

    private static final int GROUP_ID = 12;
    private static String centralIp = "35.230.146.225";
    private static int centralPort = 7500;
    private static int svcPort = 6000;

    public static MessageBroadCast messageBroadCast;

    public static void main(String[] args)
    {
        if(args.length > 0){
            centralIp = args[0].isEmpty() ? centralIp : args[0];
            //centralPort = args[1].isEmpty() ? centralPort : Integer.parseInt( args[1] );
        }
        System.out.println("Central Server ip: " + centralIp + " ,Central Server port:  " + centralPort);


        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(centralIp, centralPort)
                .usePlaintext()
                .build();

        Track track = Track.newBuilder()
                .setGroup(GROUP_ID)
                .setInPoint(1)
                .setOutPoint(2).build();

        try{
            Tariff tariff = CentralServiceGrpc
                    .newBlockingStub(channel)
                    //Set timeout
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .payment(track);

            System.out.println("Central service is OK! " + tariff);
        }catch (StatusRuntimeException ex){
            return;
        }

        veiculosEstrada = new HashMap<>();



        try{
             io.grpc.Server svc = ServerBuilder
                    .forPort(svcPort)
                    .addService(new ControlService(veiculosEstrada,channel, GROUP_ID ))
                    .build();

            svc.start();


            messageBroadCast = new MessageBroadCast();
            new Thread(messageBroadCast).start();

            System.out.println("Grpc Server started, listening on " + svcPort);
            Scanner scan= new Scanner(System.in); scan.nextLine();
            svc.shutdown();
        } catch(Exception ex) { ex.printStackTrace(); }
    }


}
