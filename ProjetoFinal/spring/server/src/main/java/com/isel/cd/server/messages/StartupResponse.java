package com.isel.cd.server.messages;

import com.isel.cd.server.DataEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class StartupResponse extends BaseMessage implements Serializable {
    private final List<DataEntity.DataDto> dataEntityList;

    public StartupResponse(List<DataEntity.DataDto> dataEntityList) {
        super(TYPE.STARTUP_RESPONSE);
        this.dataEntityList = dataEntityList;
    }
}
