import io.grpc.stub.StreamObserver;
import rpcstubs.WarnMsg;

import java.util.UUID;

public class ServerObserver implements StreamObserver<WarnMsg>
{

    private final StreamObserver<WarnMsg> clientObserver;
    public final UUID observerId;

    public ServerObserver(final StreamObserver<WarnMsg> clientObserver)
    {
        this.clientObserver = clientObserver;
        this.observerId = UUID.randomUUID();
    }

    @Override
    public void onNext(WarnMsg warnMsg)
    {
        Service.messageBroadCast.messages.add(warnMsg);
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onCompleted() {
        Service.messageBroadCast.clientsObservers.remove(observerId);
        this.clientObserver.onCompleted();
    }
}
