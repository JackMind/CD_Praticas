import io.grpc.stub.StreamObserver;
import rpcstubs.WarnMsg;

import java.util.UUID;

public class ServerObserver implements StreamObserver<WarnMsg> {

    private final StreamObserver<WarnMsg> clientObserver;
    public final UUID observerId;

    public ServerObserver(final StreamObserver<WarnMsg> clientObserver) {
        this.clientObserver = clientObserver;
        this.observerId = UUID.randomUUID();
    }

    @Override
    public void onNext(WarnMsg warnMsg)
    {
        //System.out.println("New warning received! " + warnMsg + " on observerId " + this.observerId);
        Server.messageBroadCast.messages.add(warnMsg);
        //System.out.println("Messages size: "+Server.messageBroadCast.messages.size());
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onCompleted() {
        Server.messageBroadCast.clientsObservers.remove(observerId);
        this.clientObserver.onCompleted();
    }
}
