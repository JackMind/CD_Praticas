import io.grpc.stub.StreamObserver;
import rpcsclientstubs.Data;
import rpcstubs.WarnMsg;

public class ClientServiceObserver implements StreamObserver<Data> {

    @Override
    public void onNext(Data value) {
        System.out.println(value);
    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onCompleted() {
        System.out.println("completed");
    }
}
