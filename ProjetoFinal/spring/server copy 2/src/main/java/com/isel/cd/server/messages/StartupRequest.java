package com.isel.cd.server.messages;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.STARTUP_REQUEST;

public class StartupRequest extends BaseMessage implements Serializable {
    public StartupRequest() {
        super(STARTUP_REQUEST);
    }
}
