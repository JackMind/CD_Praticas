import io.grpc.stub.StreamObserver;
import rpcstubs.WarnMsg;

public class ServerObserver implements StreamObserver<WarnMsg> {

    @Override
    public void onNext(WarnMsg warnMsg) {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onCompleted() {

    }
}
