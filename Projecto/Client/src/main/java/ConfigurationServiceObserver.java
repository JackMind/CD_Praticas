import io.grpc.stub.StreamObserver;
import rpcstubs.WarnMsg;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConfigurationServiceObserver implements StreamObserver <WarnMsg> {

    public AtomicBoolean isCompleted = new AtomicBoolean(false);

    @Override
    public void onNext(WarnMsg warnMsg) {
        System.out.println("New Update Received From Configuration Service...");
        System.out.println(warnMsg);

        Cliente.availableServers.clear();
        String[] activeServers = warnMsg.getWarning().split(";");
        for(String server : activeServers){
            if(!server.split("#")[1].equals("Service")) {

                Server server1 = new Server(server.split("#")[1], server.split("#")[2], 9001);
                System.out.println("Server added: " + server1);
                Cliente.availableServers
                        .add(server1);
            }
        }
        System.out.println(Cliente.availableServers);
        Cliente.connectToServer();
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
