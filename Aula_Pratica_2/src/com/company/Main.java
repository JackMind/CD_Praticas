package com.company;


import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


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
            svc.initLeilao(someObject, new INotificationIMPL());


            svc.licitar("1", new INotificationIMPL());


            SomeObject[] objectsArray = svc.getAllLeiloes();
            for(SomeObject object : objectsArray)
            {
                System.out.println(object.Id);
            }








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
