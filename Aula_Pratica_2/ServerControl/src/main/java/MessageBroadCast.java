import io.grpc.stub.StreamObserver;
import rpcstubs.Initial;
import rpcstubs.WarnMsg;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageBroadCast implements Runnable{

    public final Map<UUID, ServerObserver> clientsObservers;
    public final Deque<WarnMsg> messages;

    private MessageBroadCast() {
        this.clientsObservers = new ConcurrentHashMap<>();
        this.messages = new ArrayDeque<>();
    }

    private static MessageBroadCast _instance;

    public static MessageBroadCast getInstance(){
        if(_instance == null){
            return new MessageBroadCast();
        }
        return _instance;
    }


    @Override
    public void run() {
        System.out.println("MessageBroadCast thread started!");
        while (true){

            while (!this.messages.isEmpty()){
                System.out.println("Got messages!");
                final WarnMsg warnMsg = this.messages.pop();
                System.out.println("Sending " + warnMsg);
                this.clientsObservers.forEach((uuid, observer) -> observer.onNext(warnMsg));
            }

        }
    }
}
