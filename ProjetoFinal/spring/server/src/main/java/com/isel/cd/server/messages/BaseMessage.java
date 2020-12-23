package com.isel.cd.server.messages;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class BaseMessage implements Serializable {
    private TYPE type;

    public BaseMessage(TYPE type) {
        this.type = type;
    }

    public enum TYPE{
        NEW_LEADER,
        WHO_IS_LEADER,
        APPEND_DATA,
        CONSENSUS_VOTING;
    }

}
