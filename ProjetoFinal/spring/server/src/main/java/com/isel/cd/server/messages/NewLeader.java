package com.isel.cd.server.messages;

import lombok.*;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.APPEND_DATA;
import static com.isel.cd.server.messages.BaseMessage.TYPE.NEW_LEADER;

@Data
public class NewLeader extends BaseMessage implements Serializable {
    private String ip;
    private int port;
    private String serverName;

    public NewLeader() {
        super(NEW_LEADER);
    }

    public NewLeader(String ip, int port, String serverName) {
        super(NEW_LEADER);
        this.ip = ip;
        this.port = port;
        this.serverName = serverName;
    }
}