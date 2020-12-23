package com.isel.cd.configurationservice;

import lombok.AllArgsConstructor;
import spread.*;
@AllArgsConstructor
public class MessageListener implements BasicMessageListener {
    private final ClientManager clientManager;

    @Override
    public void messageReceived(SpreadMessage spreadMessage) {
        try
        {

            if (spreadMessage.isMembership()) {
                MembershipInfo info = spreadMessage.getMembershipInfo();
                //System.out.println(info.getGroupID());
                SpreadGroup group = info.getGroup();
                SpreadGroup[] members = info.getMembers();


                if (info.isRegularMembership()) {
                    if (info.isCausedByJoin()) {
                        System.out.println(info.getJoined() + " JOINED GROUP");
                        clientManager.addSpreadServer(info.getJoined());
                        clientManager.printSpreadServers();
                    } else if (info.isCausedByLeave()) {
                        System.out.println(info.getLeft() + " LEFT GROUP");
                        clientManager.removeSpreadServer(info.getLeft());
                        clientManager.printSpreadServers();
                    } else if (info.isCausedByDisconnect()) {
                        System.out.println(info.getDisconnected() + " DISCONECTED");
                        clientManager.removeSpreadServer(info.getDisconnected());
                        clientManager.printSpreadServers();
                    }
                } else if (info.isTransition()) {
                    System.out.println("TRANSITIONAL membership for group " + group);
                } else if (info.isSelfLeave()) {
                    System.out.println("SELF-LEAVE message for group " + group);
                    clientManager.removeSpreadServer(info.getLeft());
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
