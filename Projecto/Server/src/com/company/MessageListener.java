package com.company;

import spread.*;

public class MessageListener implements BasicMessageListener {
    private final LeaderManagerInterface leaderManagerInterface;
    public MessageListener(LeaderManagerInterface leaderManagerInterface) {
        this.leaderManagerInterface = leaderManagerInterface;
    }
    @Override
    public void messageReceived(SpreadMessage spreadMessage) {
        try {
            //System.out.println(spreadMessage.getType());
            if(spreadMessage.isRegular())
            {
                BaseMessage message = (BaseMessage)spreadMessage.getObject();
                if(message.getType().equals(BaseMessage.TYPE.NEW_LEADER)){
                    LeaderManager.NewLeader newLeader = (LeaderManager.NewLeader) message;
                    //System.out.println(spreadMessage.getSender() + ": " + newLeader);
                    leaderManagerInterface.selectNewLeader(newLeader);

                }else if(message.getType().equals(BaseMessage.TYPE.WHO_IS_LEADER)){
                    //System.out.println("WHO IS LEADER MESSAGE: " + message);
                    leaderManagerInterface.notifyParticipants();
                }
            }
            else if(spreadMessage.isMembership()){
                MembershipInfo info = spreadMessage.getMembershipInfo();
                SpreadGroup group = info.getGroup();

                if(info.isRegularMembership())
                {
                    if(info.isCausedByJoin())
                    {
                        System.out.println(info.getJoined() + " JOINED GROUP"); 
                        if(info.getMembers().length == 1){
                            System.out.println("I'm first, I'm leader!");
                            leaderManagerInterface.firstLeader(info.getJoined());
                        }
                        else if(!leaderManagerInterface.doIHaveLeader()){
                            System.out.println("Just joined party, who is leader?");
                            leaderManagerInterface.whoIsLeader();
                        }

                    }
                    else if(info.isCausedByLeave())
                    {
                        System.out.println(info.getLeft() + " LEFT GROUP");
                        leaderManagerInterface.notifyServerLeave(info.getLeft(), info);
                    }
                    else if(info.isCausedByDisconnect())
                    {
                        System.out.println(info.getDisconnected() + " DISCONECTED");
                        leaderManagerInterface.notifyServerLeave(info.getDisconnected(), info);
                    }
                }
            }
            //PrintMessages.MessageDetails(spreadMessage);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
