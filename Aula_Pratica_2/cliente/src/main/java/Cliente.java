import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import rpcstubs.*;

import java.util.concurrent.TimeUnit;

public class Cliente {

    static String svcIP="localhost";
    static int svcPort=6000;

    static ManagedChannel channel;

    public static void main(String[] args) {

        if(args.length > 0){
            svcIP = args[0].isEmpty() ? svcIP : args[0];
            svcPort = args[1].isEmpty() ? svcPort : Integer.parseInt( args[1] );
        }
        System.out.println("Server ip: " + svcIP + " ,Server port:  " + svcPort);

        channel = ManagedChannelBuilder
                .forAddress(svcIP, svcPort)
                .usePlaintext().build();

        enter("6543", 1);
        normalBehaviour("123456", 1, 3);

    }

    public static void normalBehaviour(String ID, int INIT_POINT, int OUT_POINT){
        try{
            enter(ID, INIT_POINT);

            StreamObserver<WarnMsg> warningObserver = ControlServiceGrpc
                    .newStub(channel)
                    .warning(new WarningObserver());

            sendWarning(warningObserver, ID, "Danger on the road "
                    + ID + " entered on the " + INIT_POINT + " entry");

            System.out.println(ID + " Doing stunts... ");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) { e.printStackTrace(); }

            sendWarning(warningObserver, ID,ID + " exiting on point " + OUT_POINT);

            Payment payment = leave(ID, OUT_POINT);

            sendWarning(warningObserver, ID, ID + " payed " + payment);

            warningObserver.onCompleted();
        }catch (Exception ex){
            System.out.println(ex);
        }
    }
    public static void enter(String id, int initPoint){
        Initial initial = Initial
                .newBuilder().setId(id).setInPoint(initPoint).build();

        try {
            ControlServiceGrpc
                    .newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .enter(initial);
        }catch (StatusRuntimeException ex){
            System.out.println("Server control did not respond, there is a ghost rider on the road!");
            throw ex;
        }
        System.out.println("Client " +initial.getId() + " initiated ride on " + initial.getInPoint() + " position");
    }

    public static void sendWarning(StreamObserver<WarnMsg> warningObserver, String id, String warning){
        WarnMsg finalWarnMsg = WarnMsg.newBuilder()
                .setId(id)
                .setWarning(warning).build();

        warningObserver.onNext(finalWarnMsg);
    }

    public static Payment leave(String id, int outPoint){
        FinalPoint finalPoint = FinalPoint.newBuilder().setId(id).setOutPoint(outPoint).build();
        Payment payment;
        try{
            payment = ControlServiceGrpc
                    .newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .leave(finalPoint);
        }catch (StatusRuntimeException ex){
            System.out.println("Control or Central server did not respond, car did not have to pay :) !");
            return null;
        }
        System.out.println("Received payment! " + payment);
        return payment;
    }
}
