package com.isel.cd.server.messages;

import com.isel.cd.server.DataEntity;
import lombok.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.RESPONSE_DATA_FROM_LEADER;

@Data
public class ResponseDataFromLeader extends BaseMessage implements Serializable {
    private DataEntity data;

    public ResponseDataFromLeader() {
        super(RESPONSE_DATA_FROM_LEADER);
    }
    public ResponseDataFromLeader(DataEntity dataEntity) {
        super(RESPONSE_DATA_FROM_LEADER);
        this.data = dataEntity;
    }
    public ResponseDataFromLeader(String key) {
        super(RESPONSE_DATA_FROM_LEADER);
        this.data = new DataEntity(key);
    }
}
