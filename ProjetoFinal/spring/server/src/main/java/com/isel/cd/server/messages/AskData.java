package com.isel.cd.server.messages;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.ASK_DATA;

@Data
public class AskData extends BaseMessage implements Serializable {
    private String key;

    public AskData() {
        super(ASK_DATA);
    }

    public AskData(String key) {
        super(ASK_DATA);
        this.key = key;
    }
}
