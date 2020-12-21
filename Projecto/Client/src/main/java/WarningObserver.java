import io.grpc.stub.StreamObserver;
import rpcstubs.WarnMsg;

import java.util.concurrent.atomic.AtomicBoolean;

public class WarningObserver implements StreamObserver <WarnMsg> {

    public AtomicBoolean isCompleted = new AtomicBoolean(false);

    @Override
    public void onNext(WarnMsg warnMsg) {
        System.out.println("\n== SERVICE MESSAGE==\n\n" + warnMsg.getWarning());
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Server crashed " + throwable);
    }

    @Override
    public void onCompleted() {
        isCompleted.set(true);
        System.out.println("Server called on complete");
    }
}
