package com.isel.cd.server;


import com.isel.cd.server.messages.AppendData;
import com.isel.cd.server.messages.ConsensusVoting;
import com.isel.cd.server.messages.NewLeader;
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

    void updateMembersSize(int length);

    void handleVoteRequest(ConsensusVoting voting);
}
