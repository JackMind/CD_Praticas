package com.company;

import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.Arrays;

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
                SpreadGroup group = info.getGroup();

                if(info.isRegularMembership())
                {
                    if(info.isCausedByJoin())
                    {
                        System.out.println(info.getJoined() + " JOINED GROUP");
                    }
                    else if(info.isCausedByLeave())
                    {
                        System.out.println(info.getLeft() + " LEFT GROUP");
                    }
                    else if(info.isCausedByDisconnect())
                    {
                        System.out.println(info.getDisconnected() + " DISCONECTED");
                    }
                }
                else if(info.isTransition())
                {
                    System.out.println("TRANSITIONAL membership for group " + group);
                }
                else if(info.isSelfLeave())
                {
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
