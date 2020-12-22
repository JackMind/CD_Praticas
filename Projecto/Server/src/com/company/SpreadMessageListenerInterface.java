package com.company;

import com.company.messages.AppendData;
import com.company.messages.NewLeader;
import spread.MembershipInfo;
import spread.SpreadGroup;

public interface SpreadMessageListenerInterface {

    void notifyServerLeave(SpreadGroup server, MembershipInfo info);

    void assignNewLeader(NewLeader newLeader);

    void assignFirstLeader(SpreadGroup server);

    void requestWhoIsLeader();

    void whoIsLeaderRequestedNotifyParticipantsWhoIsLeader();

    boolean doIHaveLeader();

    void appendDataReceived(AppendData appendData);
}
