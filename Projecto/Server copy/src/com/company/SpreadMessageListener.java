package com.company;

import com.company.messages.AppendData;
import com.company.messages.BaseMessage;
import com.company.messages.ConsensusVoting;
import com.company.messages.NewLeader;
import spread.*;

public class SpreadMessageListener implements BasicMessageListener {
    private final SpreadMessageListenerInterface spreadMessageListenerInterface;
    public SpreadMessageListener(SpreadMessageListenerInterface spreadMessageListenerInterface) {
        this.spreadMessageListenerInterface = spreadMessageListenerInterface;
    }

    @Override
    public void messageReceived(SpreadMessage spreadMessage) {
        try {
            //System.out.println(spreadMessage.getType());
            if(spreadMessage.isRegular())
            {
                BaseMessage message = (BaseMessage)spreadMessage.getObject();
                if(message.getType().equals(BaseMessage.TYPE.NEW_LEADER)){
                    NewLeader newLeader = (NewLeader) message;
                    //System.out.println(spreadMessage.getSender() + ": " + newLeader);
                    spreadMessageListenerInterface.assignNewLeader(newLeader);

                }else if(message.getType().equals(BaseMessage.TYPE.WHO_IS_LEADER)){
                    //System.out.println("WHO IS LEADER MESSAGE: " + message);
                    spreadMessageListenerInterface.whoIsLeaderRequestedNotifyParticipantsWhoIsLeader();
                }else if(message.getType().equals(BaseMessage.TYPE.APPEND_DATA)){
                    AppendData appendData = (AppendData) message;
                    spreadMessageListenerInterface.appendDataReceived(appendData);
                }else if (message.getType().equals(BaseMessage.TYPE.CONSENSUS_VOTING)){
                    ConsensusVoting voting = (ConsensusVoting) message;
                    spreadMessageListenerInterface.handleVoteRequest(voting);
                }
            }
            else if(spreadMessage.isMembership()){
                MembershipInfo info = spreadMessage.getMembershipInfo();
                spreadMessageListenerInterface.updateMembersSize(info.getMembers().length);

                //SpreadGroup group = info.getGroup();

                if(info.isRegularMembership())
                {
                    if(info.isCausedByJoin())
                    {
                        System.out.println(info.getJoined() + " JOINED GROUP"); 
                        if(info.getMembers().length == 2){
                            System.out.println("I'm first, I'm leader!");
                            spreadMessageListenerInterface.assignFirstLeader(info.getJoined());
                        }
                        else if(!spreadMessageListenerInterface.doIHaveLeader()){
                            System.out.println("Just joined party, who is leader?");
                            spreadMessageListenerInterface.requestWhoIsLeader();
                        }

                    }
                    else if(info.isCausedByLeave())
                    {
                        System.out.println(info.getLeft() + " LEFT GROUP");
                        spreadMessageListenerInterface.notifyServerLeave(info.getLeft(), info);
                    }
                    else if(info.isCausedByDisconnect())
                    {
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
