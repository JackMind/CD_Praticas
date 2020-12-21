package com.company;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Server1
{

    public static void main(String[] args)
    {
        System.out.println();
        System.out.println();
        try
        {
            int port = 4803;
            String groupID = "GRUPO123";

            //Criar Conexaão e Juntar
            SpreadConnection connection = new SpreadConnection();
            connection.connect(InetAddress.getLocalHost(), port, "Server1", false, true);

            //Listener das mensagens multicast
            MessageListener msgHandling = new MessageListener(connection);
            connection.add(msgHandling);


            //Criar Grupo e Juntar
            SpreadGroup group = new SpreadGroup();
            group.join(connection, groupID);


            //Criar Mensagem
            //SpreadMessage message = new SpreadMessage();
            //message.setObject("Ola Mundo");
            //message.addGroup(groupID);     //definir grupo de envio da mensagem
            //message.setReliable();
            //connection.multicast(message); //Enviar Mensagem Multicast


            Scanner myObj = new Scanner(System.in);
            myObj.nextLine();


            connection.disconnect();
        }
        catch (SpreadException | UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
