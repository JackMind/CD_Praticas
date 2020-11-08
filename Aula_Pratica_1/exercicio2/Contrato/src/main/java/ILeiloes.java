import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ILeiloes extends Remote {
     void initLeilao(SomeObject someObject, INotification iNotification) throws RemoteException;

     SomeObject[] getAllLeiloes() throws RemoteException;

     void licitar(String var1,float valor, INotification var2) throws RemoteException;
}

