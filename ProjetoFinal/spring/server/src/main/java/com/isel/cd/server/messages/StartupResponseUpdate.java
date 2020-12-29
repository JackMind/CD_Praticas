package com.isel.cd.server.messages;

import com.isel.cd.server.DataEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

import static com.isel.cd.server.messages.BaseMessage.TYPE.STARTUP_RESPONSE_UPDATE;

@Data
public class StartupResponseUpdate extends BaseMessage implements Serializable {
    private List<DataEntity.DataDto> dataDtoList;

    public StartupResponseUpdate() {
        super(STARTUP_RESPONSE_UPDATE);
    }

    public StartupResponseUpdate(List<DataEntity.DataDto> dataDtoList) {
        super(STARTUP_RESPONSE_UPDATE);
        this.dataDtoList = dataDtoList;
    }
}
