package com.company;

import playgamecontract.Bet;
import playgamecontract.IPlayGame;
import playgamecontract.Reply;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class Main
{
    static String serverIP = "35.230.146.225";
    static int registerPort = 7000;

    public static void main(String[] args)
    {
        try
        {
            Registry registry = LocateRegistry.getRegistry(serverIP, registerPort);
            IPlayGame svc =(IPlayGame)registry.lookup("GameServer");

            for(int x = 0; x <= 6; x++)
            {
                for(int y = 0; y <= 6; y++)
                {
                    Bet bet = new Bet(47926, x, y);
                    Reply reply = svc.playGame(bet);

                    System.out.println("=============================");
                    System.out.println("X: " + x + " | Y: " + y);
                    System.out.println("No Tries: " + reply.getNtries());
                    System.out.println("Success: " + reply.isSuccess());
                    System.out.println("Thing: " + reply.getThing());
                    System.out.println("=============================");
                    System.out.println();
                    System.out.println();
                }
            }


        //perola 1 - X:2 Y:1
        //perola 2 - X:4 Y:2
        //perola 3 - X:6 Y:0


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
