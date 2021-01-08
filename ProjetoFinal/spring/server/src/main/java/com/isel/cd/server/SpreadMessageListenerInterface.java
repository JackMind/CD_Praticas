package com.isel.cd.server;


import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadMessage;

public interface SpreadMessageListenerInterface {

    void askDataResponse(SpreadMessage spreadMessage) throws SpreadException;

    void askDataResponseReceived(SpreadMessage spreadMessage) throws SpreadException;

    void checkStartup(MembershipInfo info) throws SpreadException;

    void sendStartupData(SpreadMessage spreadMessage) throws SpreadException;

    void startupDataReceived(SpreadMessage spreadMessage) throws SpreadException;

    void updateNumberOfParticipants(int numberOfParticipants);

    void wantToWriteResponse(SpreadMessage spreadMessage) throws SpreadException;

    void wantToWriteReceived(SpreadMessage spreadMessage) throws SpreadException;

    void conflictReceived(SpreadMessage spreadMessage) throws SpreadException;
}
