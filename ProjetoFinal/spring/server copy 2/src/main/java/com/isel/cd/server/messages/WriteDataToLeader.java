package com.isel.cd.server.messages;

import com.isel.cd.server.DataEntity;
import lombok.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.WRITE_DATA_TO_LEADER;

@Data
public class WriteDataToLeader  extends BaseMessage implements Serializable {
    private final DataEntity dataEntity;

    public WriteDataToLeader() {
        super(WRITE_DATA_TO_LEADER);
        this.dataEntity = null;
    }
    public WriteDataToLeader(DataEntity dataEntity) {
        super(WRITE_DATA_TO_LEADER);
        this.dataEntity = dataEntity;
    }
}
