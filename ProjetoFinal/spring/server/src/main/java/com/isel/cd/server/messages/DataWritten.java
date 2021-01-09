package com.isel.cd.server.messages;

import lombok.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.DATA_WRITTEN;

@Data
public class DataWritten extends BaseMessage implements Serializable {
    private String key;

    public DataWritten(){super(DATA_WRITTEN);}

    public DataWritten(String key) {
        super(DATA_WRITTEN);
        this.key = key;
    }
}
