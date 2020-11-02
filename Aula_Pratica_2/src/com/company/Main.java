package com.company;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class Main
{
    static String serverIP = "localhost";
    static int registerPort = 7000;

    public static void main(String[] args)
    {
        try
        {
            Registry registry = LocateRegistry.getRegistry(serverIP, registerPort);
            ILeiloes svc = (ILeiloes) registry.lookup("Batatas");

            int port = 7002;

            startLeilao("1", svc, port++);
            startLeilao("2", svc, port++);
            startLeilao("3", svc, port++);

            SomeObject[] objectsArray = svc.getAllLeiloes();
            for(SomeObject object : objectsArray)
            {
                System.out.println(object.Id);
            }

            svc.licitar("1", new INotificationIMPL("1", port++));
            svc.licitar("2", new INotificationIMPL("2", port++));
            svc.licitar("2", new INotificationIMPL("3", port++));


        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
        catch (Exception ex)
        {
            System.err.println("Client unhandled exception: " + ex.toString());
        }
    }

    public static void startLeilao(String n, ILeiloes svc, int port) throws RemoteException {
        SomeObject someObject = new SomeObject();
        someObject.Id = n;
        String result = svc.initLeilao(someObject, new INotificationIMPL(n, port));
        System.out.println(result);
    }
}
