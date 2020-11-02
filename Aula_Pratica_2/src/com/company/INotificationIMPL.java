package com.company;

import java.rmi.RemoteException;


public class INotificationIMPL implements INotification
{
    @Override
    public void sendNotification(String s) throws RemoteException
    {
        System.out.println("Teste: " + s);
    }
}
