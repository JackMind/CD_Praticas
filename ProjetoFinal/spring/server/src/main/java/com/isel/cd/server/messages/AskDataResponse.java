package com.isel.cd.server.messages;

import com.isel.cd.server.DataEntity;
import lombok.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.ASK_DATA_RESPONSE;

@Data
public class AskDataResponse extends BaseMessage implements Serializable {

    private DataEntity.DataDto dataDto;

    public AskDataResponse() {
        super(ASK_DATA_RESPONSE);
    }

    public AskDataResponse(DataEntity dataDto) {
        super(ASK_DATA_RESPONSE);
        this.dataDto = new DataEntity.DataDto(dataDto.getKey(), dataDto.getData().getData());
    }
}
