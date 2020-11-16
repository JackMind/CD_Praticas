import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Cliente {

    static String svcIP="35.230.146.225";
    static int svcPort=6000;

    public static void main(String[] args) {

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(svcIP, svcPort)
                .usePlaintext().build();
        // call pingServer
        Controlo
        Msg request = Msg.newBuilder().setTxt("luis").build();
        ServiceAulaGrpc.ServiceAulaBlockingStub blockingStub = 	ServiceAulaGrpc.newBlockingStub(channel);
        Msg rpy = blockingStub.pingServer(request);
        System.out.println("result =" + rpy.getTxt());

    }
}
