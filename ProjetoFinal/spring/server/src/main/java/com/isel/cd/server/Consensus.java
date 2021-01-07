package com.isel.cd.server;

import lombok.Data;

@Data
public class Consensus {

    private WRITE_STATE myState;

    public enum WRITE_STATE{
        IDLE,
        WRITING;
    }
}
