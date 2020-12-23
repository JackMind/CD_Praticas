package com.isel.cd.server.messages;

import com.isel.cd.server.DataEntity;
import rpcsclientstubs.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.APPEND_DATA;


@lombok.Data
public class AppendData extends BaseMessage implements Serializable {
    private String key;
    private DataEntity.Data data;

    public AppendData() {
        super(APPEND_DATA);
    }

    public AppendData(Data request){
        super(APPEND_DATA);
        this.key = request.getKey();
        this.data = new DataEntity.Data( request.getData() );
    }

    public AppendData(String key, DataEntity.Data data) {
        super(APPEND_DATA);
        this.key = key;
        this.data = data;
    }

}
