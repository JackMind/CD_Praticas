package com.isel.cd.server;


import com.isel.cd.server.messages.BaseMessage;
import lombok.extern.slf4j.Slf4j;
import spread.BasicMessageListener;
import spread.MembershipInfo;
import spread.SpreadMessage;

@Slf4j
public class SpreadMessageListener implements BasicMessageListener {
    private final SpreadMessageListenerInterface spreadMessageListenerInterface;
    public SpreadMessageListener(SpreadMessageListenerInterface spreadMessageListenerInterface) {
        this.spreadMessageListenerInterface = spreadMessageListenerInterface;
    }

    @Override
    public void messageReceived(SpreadMessage spreadMessage) {
        try {
            if(spreadMessage.isRegular()) {
                BaseMessage message = (BaseMessage)spreadMessage.getObject();
                switch (message.getType()){
                    case ASK_DATA:
                        spreadMessageListenerInterface.askDataResponse(spreadMessage);
                        break;
                    case ASK_DATA_RESPONSE:
                        spreadMessageListenerInterface.askDataResponseReceived(spreadMessage);
                        break;
                    case STARTUP_REQUEST_UPDATE:
                        spreadMessageListenerInterface.sendStartupData(spreadMessage);
                        break;
                    case STARTUP_RESPONSE_UPDATE:
                        spreadMessageListenerInterface.startupDataReceived(spreadMessage);
                        break;
                    case WANT_TO_WRITE:
                        spreadMessageListenerInterface.wantToWriteReceived(spreadMessage);
                        break;
                    case WANT_TO_WRITE_RESPONSE:
                        spreadMessageListenerInterface.wantToWriteResponse(spreadMessage);
                        break;
                    case WRITE_CONFLICT:
                        spreadMessageListenerInterface.conflictReceived(spreadMessage);
                        break;
                    default:
                        log.warn("Unknown regular message type.");
                }
            }
            else if(spreadMessage.isMembership()){
                MembershipInfo info = spreadMessage.getMembershipInfo();
                spreadMessageListenerInterface.updateNumberOfParticipants(info.getMembers().length);

                if(info.isRegularMembership()) {
                    if(info.isCausedByJoin()) {
                        log.debug( "{} JOINED GROUP", info.getJoined());
                        //Configuration service already up
                        if(info.getMembers().length >= 2) {
                            spreadMessageListenerInterface.checkStartup(info);
                        }
                    } else if(info.isCausedByLeave()) {
                        log.debug( "{} LEFT GROUP", info.getLeft());
                    }else if(info.isCausedByDisconnect()) {
                        log.debug( "{} DISCONECTED", info.getDisconnected());
                    }
                }
            }
        } catch(Exception e) {
            log.error("Error on Spread Message Listener", e);
        }
    }
}
