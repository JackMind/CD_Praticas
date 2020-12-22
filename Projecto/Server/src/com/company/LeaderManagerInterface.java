package com.company;

import spread.MembershipInfo;
import spread.SpreadGroup;

public interface LeaderManagerInterface {

    void notifyServerLeave(SpreadGroup server, MembershipInfo info);

    void selectNewLeader(LeaderManager.NewLeader newLeader);

    void firstLeader(SpreadGroup server);

    void whoIsLeader();

    void notifyParticipants();

    boolean doIHaveLeader();

}
