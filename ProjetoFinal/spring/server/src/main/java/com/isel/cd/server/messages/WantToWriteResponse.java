package com.isel.cd.server.messages;

import lombok.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.WANT_TO_WRITE;
import static com.isel.cd.server.messages.BaseMessage.TYPE.WANT_TO_WRITE_RESPONSE;

@Data
public class WantToWriteResponse extends BaseMessage implements Serializable {
    private String key;

    public WantToWriteResponse(){super(WANT_TO_WRITE_RESPONSE);}

    public WantToWriteResponse(String key) {
        super(WANT_TO_WRITE_RESPONSE);
        this.key = key;
    }
}
