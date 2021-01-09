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
        REVALIDATE_DATA,
        DATA_WRITTEN,
        WRITE_CONFLICT,
        WANT_TO_WRITE_RESPONSE,
        WANT_TO_WRITE,
        ASK_DATA,
        ASK_DATA_RESPONSE,
        STARTUP_REQUEST_UPDATE,
        STARTUP_RESPONSE_UPDATE;
    }

}
