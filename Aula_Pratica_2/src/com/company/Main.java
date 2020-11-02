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


            SomeObject someObject = new SomeObject();
            someObject.Id = "1";
            String result = svc.initLeilao(someObject, new INotificationIMPL(7002));

            System.out.println(result);

            SomeObject[] objectsArray = svc.getAllLeiloes();
            for(SomeObject object : objectsArray)
            {
                System.out.println(object.Id);
            }

            svc.licitar("1", new INotificationIMPL(7003));





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
}
