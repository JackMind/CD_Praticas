import io.grpc.stub.StreamObserver;
import rpcstubs.Initial;
import rpcstubs.WarnMsg;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageBroadCast implements Runnable{

    public final Map<UUID, StreamObserver<WarnMsg>> clientsObservers;
    public final Deque<WarnMsg> messages;

    MessageBroadCast() {
        this.clientsObservers = new ConcurrentHashMap<>();
        this.messages = new ArrayDeque<>();
    }

    @Override
    public void run()
    {
        System.out.println("MessageBroadCast thread started!");
        while (true)
        {
            if(!this.messages.isEmpty())
            {
                System.out.println("Got messages!");
                final WarnMsg warnMsg = this.messages.pop();

                for(Map.Entry<UUID,StreamObserver<WarnMsg>> observer : clientsObservers.entrySet())
                {
                    System.out.println("Broadcasting to " + observer.getKey());
                    observer.getValue().onNext(warnMsg);
                }
            }

            try {
                Thread.sleep(2*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
