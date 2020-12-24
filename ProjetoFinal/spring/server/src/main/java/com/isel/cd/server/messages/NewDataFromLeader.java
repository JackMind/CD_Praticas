package com.isel.cd.server.messages;

import com.isel.cd.server.DataEntity;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.NEW_DATA_FROM_LEADER;


@lombok.Data
public class NewDataFromLeader extends BaseMessage implements Serializable {
    private DataEntity data;

    public NewDataFromLeader() {
        super(NEW_DATA_FROM_LEADER);
    }

    public NewDataFromLeader(DataEntity request){
        super(NEW_DATA_FROM_LEADER);
        this.data = request ;
    }


}
