package com.isel.cd.server.messages;

import lombok.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.REVALIDATE_DATA;

@Data
public class RevalidateData extends BaseMessage implements Serializable {
    private String key;

    public RevalidateData(){super(REVALIDATE_DATA);}

    public RevalidateData(String key) {
        super(REVALIDATE_DATA);
        this.key = key;
    }
}
