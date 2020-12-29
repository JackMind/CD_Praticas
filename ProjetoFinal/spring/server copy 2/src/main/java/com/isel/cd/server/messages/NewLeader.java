package com.isel.cd.server.messages;

import lombok.*;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.NEW_LEADER;

@Data
public class NewLeader extends BaseMessage implements Serializable {

    public NewLeader() {
        super(NEW_LEADER);
    }

}