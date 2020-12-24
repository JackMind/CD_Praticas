package com.isel.cd.server.messages;

import lombok.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.ASK_DATA_TO_LEADER;

@Data
public class AskDataToLeader extends BaseMessage implements Serializable {

    private String key;

    public AskDataToLeader() {
        super(ASK_DATA_TO_LEADER);
    }

    public AskDataToLeader(String key) {
        super(ASK_DATA_TO_LEADER);
        this.key = key;
    }
}
