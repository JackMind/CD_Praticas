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
        WRITE_CONFLICT,
        WANT_TO_WRITE_RESPONSE,
        WANT_TO_WRITE,
        NEW_LEADER,
        WHO_IS_LEADER,
        NEW_DATA_FROM_LEADER,
        ASK_DATA_TO_LEADER,
        RESPONSE_DATA_FROM_LEADER,
        WRITE_DATA_TO_LEADER,
        STARTUP_REQUEST,
        STARTUP_RESPONSE,
        ASK_DATA,
        ASK_DATA_RESPONSE,
        INVALIDATE_DATA,
        STARTUP_REQUEST_UPDATE,
        STARTUP_RESPONSE_UPDATE;
    }

}
