package com.company.messages;

import java.io.Serializable;

public class NewLeader extends BaseMessage implements Serializable {
    private final String ip;
    private final int port;
    private final String serverName;

    public NewLeader(String ip, int port, String serverName) {
        super(TYPE.NEW_LEADER);
        this.serverName = serverName;
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getServerName() {
        return serverName;
    }

    @Override
    public String toString() {
        return "NewLeader{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", serverName='" + serverName + '\'' +
                '}';
    }

}