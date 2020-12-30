package com.isel.cd.configurationservice;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spread.*;

@AllArgsConstructor
@Slf4j
public class MessageListener implements BasicMessageListener {
    private final ClientManager clientManager;

    @Override
    public void messageReceived(SpreadMessage spreadMessage) {
        try {

            if (spreadMessage.isMembership()) {
                MembershipInfo info = spreadMessage.getMembershipInfo();
                //System.out.println(info.getGroupID());
                SpreadGroup group = info.getGroup();
                SpreadGroup[] members = info.getMembers();


                if (info.isRegularMembership()) {
                    if (info.isCausedByJoin()) {
                        log.info( "{} JOINED GROUP", info.getJoined());
                        clientManager.addSpreadServer(info.getJoined());
                        clientManager.printSpreadServers();
                    } else if (info.isCausedByLeave()) {
                        log.info( "{} LEFT GROUP", info.getLeft());
                        clientManager.removeSpreadServer(info.getLeft());
                        clientManager.printSpreadServers();
                    } else if (info.isCausedByDisconnect()) {
                        log.info( "{} DISCONECTED", info.getDisconnected());
                        clientManager.removeSpreadServer(info.getDisconnected());
                        clientManager.printSpreadServers();
                    }
                } else if (info.isTransition()) {
                    log.info("TRANSITIONAL membership for group {}", group);
                } else if (info.isSelfLeave()) {
                    log.info("SELF-LEAVE message for group {}", group);
                    clientManager.removeSpreadServer(info.getLeft());
                }
            }
        } catch(Exception e) {
            log.error("Error: ",e);
            System.exit(1);
        }
    }
}
