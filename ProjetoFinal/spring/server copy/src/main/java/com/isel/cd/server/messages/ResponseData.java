package com.isel.cd.server.messages;

import com.isel.cd.server.DataEntity;
import lombok.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.RESPONSE_DATA;

@Data
public class ResponseData extends BaseMessage implements Serializable {
    private DataEntity data;

    public ResponseData() {
        super(RESPONSE_DATA);
    }
    public ResponseData(DataEntity dataEntity) {
        super(RESPONSE_DATA);
        this.data = dataEntity;
    }
    public ResponseData(String key) {
        super(RESPONSE_DATA);
        this.data = new DataEntity(key);
    }
}
