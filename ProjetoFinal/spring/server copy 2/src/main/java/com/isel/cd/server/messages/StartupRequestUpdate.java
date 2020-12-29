package com.isel.cd.server.messages;

import com.isel.cd.server.DataEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

import static com.isel.cd.server.messages.BaseMessage.TYPE.STARTUP_REQUEST_UPDATE;

@Data
public class StartupRequestUpdate extends BaseMessage implements Serializable {
    private List<DataEntity.DataDto> dataDtoList;

    public StartupRequestUpdate() {
        super(STARTUP_REQUEST_UPDATE);
    }

    public StartupRequestUpdate(List<DataEntity.DataDto> dataDtoList) {
        super(STARTUP_REQUEST_UPDATE);
        this.dataDtoList = dataDtoList;
    }
}
