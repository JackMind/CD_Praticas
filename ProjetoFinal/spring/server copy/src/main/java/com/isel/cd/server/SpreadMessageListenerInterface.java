package com.isel.cd.server;


import com.isel.cd.server.messages.AppendData;
import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

public interface SpreadMessageListenerInterface {

    void notifyServerLeave(SpreadGroup server, MembershipInfo info);

    void assignNewLeader(SpreadMessage spreadMessage) throws SpreadException;

    void assignFirstLeader(SpreadGroup server);

    void requestWhoIsLeader();

    void whoIsLeaderRequestedNotifyParticipantsWhoIsLeader();

    boolean doIHaveLeader();

    void appendDataReceived(AppendData appendData);

    void dataRequestedToLeader(SpreadMessage spreadMessage) throws SpreadException;

    void receivedResponseData(SpreadMessage spreadMessage) throws SpreadException;
}
