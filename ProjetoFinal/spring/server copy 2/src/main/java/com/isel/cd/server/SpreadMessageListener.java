package com.isel.cd.server;


import com.isel.cd.server.messages.NewDataFromLeader;
import com.isel.cd.server.messages.BaseMessage;
import spread.BasicMessageListener;
import spread.MembershipInfo;
import spread.SpreadMessage;

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
                System.out.println(message);
                switch (message.getType()){
                    case ASK_DATA_TO_LEADER:
                        spreadMessageListenerInterface.dataRequestedToLeader(spreadMessage);
                        break;
                    case RESPONSE_DATA_FROM_LEADER:
                        spreadMessageListenerInterface.receivedResponseData(spreadMessage);
                        break;
                    case NEW_DATA_FROM_LEADER:
                        spreadMessageListenerInterface.appendDataReceived(spreadMessage);
                        break;
                    case NEW_LEADER:
                        spreadMessageListenerInterface.assignNewLeader(spreadMessage);
                        break;
                    case WHO_IS_LEADER:
                        spreadMessageListenerInterface.whoIsLeaderRequestedNotifyParticipantsWhoIsLeader();
                        break;
                    case WRITE_DATA_TO_LEADER:
                        spreadMessageListenerInterface.writeDataToLeader(spreadMessage);
                        break;
                    case STARTUP_REQUEST:
                        spreadMessageListenerInterface.startupDataRequested(spreadMessage);
                        break;
                    case STARTUP_RESPONSE:
                        spreadMessageListenerInterface.startupDataResponse(spreadMessage);
                        break;
                    case ASK_DATA:
                        spreadMessageListenerInterface.askDataResponse(spreadMessage);
                        break;
                    case ASK_DATA_RESPONSE:
                        spreadMessageListenerInterface.askDataResponseReceived(spreadMessage);
                        break;
                    case INVALIDATE_DATA:
                        spreadMessageListenerInterface.invalidateData(spreadMessage);
                        break;
                    case STARTUP_REQUEST_UPDATE:
                        spreadMessageListenerInterface.sendStartupData(spreadMessage);
                        break;
                    case STARTUP_RESPONSE_UPDATE:
                        spreadMessageListenerInterface.startupDataReceived(spreadMessage);
                        break;
                    default:
                        System.out.println("Unknown regular type message");
                }
            }
            else if(spreadMessage.isMembership()){
                MembershipInfo info = spreadMessage.getMembershipInfo();
                //SpreadGroup group = info.getGroup();

                if(info.isRegularMembership()) {
                    if(info.isCausedByJoin()) {
                        System.out.println(info.getJoined() + " JOINED GROUP");
                        //Configuration service already up
                        if(spreadMessageListenerInterface.isLeaderMechanism()){
                            if(info.getMembers().length <= 2){
                                System.out.println("I'm first, I'm leader!");
                                spreadMessageListenerInterface.assignFirstLeader(info.getJoined());
                            }
                            else if(!spreadMessageListenerInterface.doIHaveLeader()){
                                System.out.println("Just joined party, who is leader?");
                                spreadMessageListenerInterface.requestWhoIsLeader();
                            }
                        }else{
                            if(info.getMembers().length >= 2) {
                                spreadMessageListenerInterface.checkStartup(info);
                            }
                        }

                    } else if(info.isCausedByLeave()) {
                        System.out.println(info.getLeft() + " LEFT GROUP");
                        spreadMessageListenerInterface.notifyServerLeave(info.getLeft(), info);
                    }else if(info.isCausedByDisconnect()) {
                        System.out.println(info.getDisconnected() + " DISCONECTED");
                        spreadMessageListenerInterface.notifyServerLeave(info.getDisconnected(), info);
                    }
                }
            }
            //PrintMessages.MessageDetails(spreadMessage);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
