import io.grpc.stub.StreamObserver;
import rpcstubs.WarnMsg;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
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
        while (true)
        {
            if(!this.messages.isEmpty())
            {
                System.out.println();
                final WarnMsg warnMsg = this.messages.pop();
                for(Map.Entry<UUID,StreamObserver<WarnMsg>> observer : clientsObservers.entrySet())
                {
                    System.out.println("Broadcasting to " + observer.getKey());
                    broadcast(warnMsg, observer.getValue());
                }
            }

            try {
                Thread.sleep(2*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void broadcast(WarnMsg warnMsg, StreamObserver<WarnMsg> to)
    {
        to.onNext(warnMsg);
    }
}
