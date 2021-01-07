package com.isel.cd.server.messages;

import com.isel.cd.server.DataEntity;
import lombok.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.WANT_TO_WRITE;

@Data
public class WantToWrite extends BaseMessage implements Serializable {
    private DataEntity.DataDto data;

    public WantToWrite(){super(WANT_TO_WRITE);}

    public WantToWrite(DataEntity.DataDto data) {
        super(WANT_TO_WRITE);
        this.data = data;
    }
}
