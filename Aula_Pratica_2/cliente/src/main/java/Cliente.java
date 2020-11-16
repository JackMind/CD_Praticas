import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import rpcstubs.*;

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

        Initial initial = Initial.newBuilder().setId("123456").setInPoint(1).build();

        ControlServiceGrpc.ControlServiceBlockingStub
                blockingStub = new ControlServiceGrpc.newBlockingStub(channel);


        blockingStub.enter(initial);
        System.out.println("Client " +initial.getId() + "inited ride on " + initial.getInPoint());


        ControlServiceGrpc.ControlServiceStub noBlockStub = ControlServiceGrpc.newStub(channel);
        WarningObserver warningObserver = new WarningObserver();


        StreamObserver<WarnMsg> serverObserver = noBlockStub.warning(warningObserver);

        WarnMsg warnMsg = WarnMsg.newBuilder()
                .setWarning("Danger on the road "
                        + initial.getId()
                        + " entered on the "
                        + initial.getInPoint()
                        + " entry").build();
        serverObserver.onNext(warnMsg);

        while (!warningObserver.isCompleted.get()) {

            System.out.println("doing something");
            Thread.sleep(2000);

        }


        FinalPoint finalPoint = FinalPoint.newBuilder().setOutPoint(3).build();

        WarnMsg finalWarnMsg = WarnMsg.newBuilder()
                .setWarning(initial.getId()
                        + " exiting on point "
                        + finalPoint.getOutPoint()).build();

        serverObserver.onNext(finalWarnMsg);


        Payment payment = blockingStub.leave(finalPoint);

        WarnMsg payWarnMsg = WarnMsg.newBuilder()
                .setWarning(initial.getId()
                        + " payed "
                        + payment.getValue()).build();
        serverObserver.onNext(payWarnMsg);

        serverObserver.onCompleted();
    }
}
