package com.isel.cd.server;


import spread.MembershipInfo;
import spread.SpreadException;
import spread.SpreadMessage;

public interface SpreadMessageListenerInterface {

    void askData(SpreadMessage spreadMessage) throws SpreadException;

    void askDataResponse(SpreadMessage spreadMessage) throws SpreadException;

    void checkStartup(MembershipInfo info) throws SpreadException;

    void startupData(SpreadMessage spreadMessage) throws SpreadException;

    void startupDataResponse(SpreadMessage spreadMessage) throws SpreadException;

    void updateNumberOfParticipants(int numberOfParticipants);

    void wantToWriteResponse(SpreadMessage spreadMessage) throws SpreadException;

    void wantToWriteReceived(SpreadMessage spreadMessage) throws SpreadException;

    void conflictReceived(SpreadMessage spreadMessage) throws SpreadException;

    void dataWritten(SpreadMessage spreadMessage) throws SpreadException;

    void revalidateData(SpreadMessage spreadMessage) throws SpreadException;
}
