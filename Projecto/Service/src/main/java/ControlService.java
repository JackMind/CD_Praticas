import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import rpcstubs.Void;
import rpcstubs.*;

import java.util.Map;

public class ControlService extends ControlServiceGrpc.ControlServiceImplBase
{
    /*private final ManagedChannel channel;
    private final int groupId;

    public ControlService(ManagedChannel channel, int groupId)
    {
        this.channel = channel;
        this.groupId = groupId;
    }*/

    @Override
    public void enter(Initial request, StreamObserver<Void> responseObserver)
    {
        Void voidResponse = Void.newBuilder().build();
        responseObserver.onNext(voidResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void leave(FinalPoint request, StreamObserver<Payment> responseObserver)
    {
        Payment pagamento = Payment.newBuilder().build();
        responseObserver.onNext(pagamento);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<WarnMsg> warning(StreamObserver<WarnMsg> responseObserver)
    {
        ServerObserver observer = new ServerObserver(responseObserver);
        Service.messageBroadCast.clientsObservers.put(observer.observerId, responseObserver);
        return observer;
    }
}
