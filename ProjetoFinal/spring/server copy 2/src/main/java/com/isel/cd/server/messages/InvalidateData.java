package com.isel.cd.server.messages;

import lombok.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.INVALIDATE_DATA;

@Data
public class InvalidateData extends BaseMessage implements Serializable {
    private String key;

    public InvalidateData() {
        super(INVALIDATE_DATA);
    }

    public InvalidateData(String key) {
        super(INVALIDATE_DATA);
        this.key = key;
    }
}
