import io.grpc.stub.StreamObserver;
import rpcstubs.Initial;
import rpcstubs.WarnMsg;

import java.util.Map;

public class ServerObserver implements StreamObserver<WarnMsg> {

    private final StreamObserver<WarnMsg> server;
    private final Map<String, Initial> veiculosEstrada;

    public ServerObserver(StreamObserver<WarnMsg> server, Map<String, Initial> veiculosEstrada) {
        this.server = server;
        this.veiculosEstrada = veiculosEstrada;
    }

    @Override
    public void onNext(WarnMsg warnMsg) {

        System.out.println("New warning received! " + warnMsg);
        for (Map.Entry<String, Initial> entry : this.veiculosEstrada.entrySet())
        {
            this.server.onNext(WarnMsg.newBuilder().setWarning(warnMsg.getWarning()).build());
        }

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onCompleted() {
        System.out.println("Called on complete!");
        this.server.onCompleted();
    }
}
