import java.rmi.Remote;
import java.rmi.RemoteException;

public interface INotification extends Remote {

    void sendNotification(String message) throws RemoteException;

}
