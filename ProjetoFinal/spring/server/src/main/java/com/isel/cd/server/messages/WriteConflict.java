package com.isel.cd.server.messages;

import com.isel.cd.server.DataEntity;
import lombok.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.WRITE_CONFLICT;

@Data
public class WriteConflict extends BaseMessage implements Serializable {
    private DataEntity.DataDto dataDto;
    public WriteConflict(){super(WRITE_CONFLICT);}

    public WriteConflict(DataEntity.DataDto dataDto) {
        super(WRITE_CONFLICT);
        this.dataDto = dataDto;
    }
}
