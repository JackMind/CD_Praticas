import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ILeiloes extends Remote {
     String initLeilao(SomeObject someObject, INotification iNotification) throws RemoteException;

     SomeObject[] getAllLeiloes() throws RemoteException;

     void licitar(String var1, INotification var2) throws RemoteException;
}

