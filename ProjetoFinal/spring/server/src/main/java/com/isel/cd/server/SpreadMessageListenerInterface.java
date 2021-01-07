package com.isel.cd.server;


import com.isel.cd.server.messages.NewDataFromLeader;
import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

public interface SpreadMessageListenerInterface {

    void notifyServerLeave(SpreadGroup server, MembershipInfo info);

    void assignNewLeader(SpreadMessage spreadMessage) throws SpreadException, InterruptedException;

    void assignFirstLeader(SpreadGroup server);

    void requestWhoIsLeader();

    void whoIsLeaderRequestedNotifyParticipantsWhoIsLeader();

    boolean doIHaveLeader();

    void appendDataReceived(SpreadMessage spreadMessage) throws SpreadException;

    void dataRequestedToLeader(SpreadMessage spreadMessage) throws SpreadException;

    void receivedResponseData(SpreadMessage spreadMessage) throws SpreadException;

    void writeDataToLeader(SpreadMessage spreadMessage) throws SpreadException;

    void startupDataRequested(SpreadMessage spreadMessage);

    void startupDataResponse(SpreadMessage spreadMessage) throws SpreadException;

    void askDataResponse(SpreadMessage spreadMessage) throws SpreadException;

    void askDataResponseReceived(SpreadMessage spreadMessage) throws SpreadException;

    void invalidateData(SpreadMessage spreadMessage) throws SpreadException;

    boolean isLeaderMechanism();

    void checkStartup(MembershipInfo info) throws SpreadException;

    void sendStartupData(SpreadMessage spreadMessage) throws SpreadException;

    void startupDataReceived(SpreadMessage spreadMessage) throws SpreadException;

    void updateNumberOfParticipants(int numberOfParticipants);

    void wantToWriteResponse(SpreadMessage spreadMessage) throws SpreadException;

    void wantToWriteReceived(SpreadMessage spreadMessage) throws SpreadException;

    void conflictReceived(SpreadMessage spreadMessage) throws SpreadException;
}
