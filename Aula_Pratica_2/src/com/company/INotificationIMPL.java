package com.company;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


public class INotificationIMPL extends UnicastRemoteObject implements INotification
{
    protected INotificationIMPL(int port) throws RemoteException {
        super(port);
    }

    @Override
    public void sendNotification(String s) throws RemoteException
    {
        System.out.println("Teste: " + s);
    }
}
