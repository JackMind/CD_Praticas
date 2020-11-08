import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class INotificationIMPL extends UnicastRemoteObject implements INotification{
    String id;
    int port;
    protected INotificationIMPL(String id, int port) throws RemoteException {
        super(port);
        this.port = port;
        this.id = id;
    }

    @Override
    public void sendNotification(String s) throws RemoteException
    {
        System.out.println("Callback for id: " + this.id + ", port:  " + this.port +"->" + s);
    }
}
