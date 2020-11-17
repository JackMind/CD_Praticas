import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import rpcstubs.*;

import java.util.concurrent.TimeUnit;

public class Cliente {

    static String svcIP="localhost";
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

        String ID = "123456";
        int INIT_POINT = 1;
        int OUT_POINT = 3;

        Initial initial = Initial.newBuilder().setId(ID).setInPoint(INIT_POINT).build();

        try {
            ControlServiceGrpc
                    .newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .enter(initial);
        }catch (StatusRuntimeException ex){
            System.out.println("Server control did not respond, there is a ghost rider on the road!");
            return;
        }

        System.out.println("Client " +initial.getId() + " initiated ride on " + initial.getInPoint() + " position");


        ControlServiceGrpc.ControlServiceStub noBlockStub = ControlServiceGrpc.newStub(channel);
        WarningObserver warningObserver = new WarningObserver();


        StreamObserver<WarnMsg> serverObserver = noBlockStub.warning(warningObserver);

        WarnMsg warnMsg = WarnMsg.newBuilder()
                .setId(ID)
                .setWarning("Danger on the road "
                        + initial.getId()
                        + " entered on the "
                        + initial.getInPoint()
                        + " entry").build();
        serverObserver.onNext(warnMsg);

        while (!warningObserver.isCompleted.get()) {

            System.out.println("doing something");

            try
            {
                Thread.sleep(2000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        FinalPoint finalPoint = FinalPoint.newBuilder().setId(ID).setOutPoint(OUT_POINT).build();

        WarnMsg finalWarnMsg = WarnMsg.newBuilder()
                .setId(ID)
                .setWarning(initial.getId()
                        + " exiting on point "
                        + finalPoint.getOutPoint()).build();

        serverObserver.onNext(finalWarnMsg);

        Payment payment;
        try{
            payment = ControlServiceGrpc
                    .newBlockingStub(channel)
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .leave(finalPoint);
        }catch (StatusRuntimeException ex){
            System.out.println("Control or Central server did not respond, car did not have to pay :) !");
            return;
        }

        System.out.println("Received payment! " + payment);

        WarnMsg payWarnMsg = WarnMsg.newBuilder()
                .setWarning(initial.getId()
                        + " payed "
                        + payment.getValue()).build();
        serverObserver.onNext(payWarnMsg);

        serverObserver.onCompleted();
    }
}
