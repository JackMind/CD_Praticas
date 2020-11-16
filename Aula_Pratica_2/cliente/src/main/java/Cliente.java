import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Cliente {

    static String svcIP="35.230.146.225";
    static int svcPort=6000;

    public static void main(String[] args) {

        if(args.length > 0){
            svcIP = args[0].isEmpty() ? svcIP : args[0];
            svcPort = args[1].isEmpty() ? svcPort : Integer.parseInt( args[1] );
        }
        System.out.println("Server ip: " + svcIP + " ,Server port:  " + svcPort);
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(svcIP, svcPort)
                .usePlaintext().build();
        // call pingServer
        rpcCon

    }
}
