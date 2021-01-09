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
                        spreadMessageListenerInterface.askData(spreadMessage);
                        break;
                    case ASK_DATA_RESPONSE:
                        spreadMessageListenerInterface.askDataResponse(spreadMessage);
                        break;
                    case STARTUP_REQUEST_UPDATE:
                        spreadMessageListenerInterface.startupData(spreadMessage);
                        break;
                    case STARTUP_RESPONSE_UPDATE:
                        spreadMessageListenerInterface.startupDataResponse(spreadMessage);
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
                    case DATA_WRITTEN:
                        spreadMessageListenerInterface.dataWritten(spreadMessage);
                        break;
                    case REVALIDATE_DATA:
                        spreadMessageListenerInterface.revalidateData(spreadMessage);
                        break;
                    default:
                        log.warn("Unknown regular message type.");
                }
            }
            else if(spreadMessage.isMembership()){
                MembershipInfo info = spreadMessage.getMembershipInfo();
                int groupSize = info.getMembers().length;
                spreadMessageListenerInterface.updateNumberOfParticipants(groupSize);

                if(info.isRegularMembership()) {
                    if(info.isCausedByJoin()) {
                        log.info( "{} JOINED GROUP. group size = {}", info.getJoined(), groupSize);
                        //Configuration service already up
                        if(groupSize >= 2) {
                            spreadMessageListenerInterface.checkStartup(info);
                        }
                    } else if(info.isCausedByLeave()) {
                        log.info( "{} LEFT GROUP. group size = {}", info.getLeft(), groupSize);
                    }else if(info.isCausedByDisconnect()) {
                        log.info( "{} DISCONECTED. group size = {}", info.getDisconnected(), groupSize);
                    }
                }
            }
        } catch(Exception e) {
            log.error("Error on Spread Message Listener", e);
        }
    }
}
