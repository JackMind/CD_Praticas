import rpcstubs.WarnMsg;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.Map;

public  class  PrintMessages
{
    static void MessageDetails(SpreadMessage msg)
    {
        try
        {
            if(msg.isRegular())
            {
                System.out.println(msg.getSender() + ": " + msg.getObject().toString());
            }
            else if (msg.isMembership())
            {
                MembershipInfo info = msg.getMembershipInfo();
                //System.out.println(info.getGroupID());
                SpreadGroup group = info.getGroup();
                SpreadGroup[] members = info.getMembers();


                if(info.isRegularMembership())
                {
                    if(info.isCausedByJoin())
                    {
                        Service.groupMembers.put(info.getJoined().toString(), info.getJoined());

                        //Service.messageBroadCast.messages.add(WarnMsg.newBuilder().setWarning("Server '" + info.getJoined().toString() + "' Connected!").build());
                        Service.messageBroadCast.messages.add(WarnMsg.newBuilder().setWarning(Service.serverList()).build());

                        System.out.println(info.getJoined() + " JOINED GROUP");
                    }
                    else if(info.isCausedByLeave())
                    {
                        Service.groupMembers.remove(info.getLeft().toString());
                        //Service.messageBroadCast.messages.add(WarnMsg.newBuilder().setWarning("Server '" + info.getLeft().toString() + "' Left!").build());

                        Service.messageBroadCast.messages.add(WarnMsg.newBuilder().setWarning(Service.serverList()).build());

                        System.out.println(info.getLeft() + " LEFT GROUP");
                        Service.imprimirMembros();
                    }
                    else if(info.isCausedByDisconnect())
                    {
                        Service.groupMembers.remove(info.getDisconnected().toString());

                        //Service.messageBroadCast.messages.add(WarnMsg.newBuilder().setWarning("Server '" + info.getDisconnected().toString() + "' Disconnected!").build());
                        Service.messageBroadCast.messages.add(WarnMsg.newBuilder().setWarning(Service.serverList()).build());

                        System.out.println(info.getDisconnected() + " DISCONECTED");
                        Service.imprimirMembros();
                    }
                }
                else if(info.isTransition())
                {
                    System.out.println("TRANSITIONAL membership for group " + group);
                }
                else if(info.isSelfLeave())
                {
                    Service.groupMembers.remove(info.getLeft().toString());
                    System.out.println("SELF-LEAVE message for group " + group);
                }
            }
            else
            {
                System.out.println("Message is of unknown type: " + msg.getServiceType());
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }


}
