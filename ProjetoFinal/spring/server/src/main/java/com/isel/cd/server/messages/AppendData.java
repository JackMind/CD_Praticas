package com.isel.cd.server.messages;

import com.isel.cd.server.Database;
import lombok.*;
import rpcsclientstubs.Data;

import java.io.Serializable;

import static com.isel.cd.server.messages.BaseMessage.TYPE.APPEND_DATA;


@lombok.Data
public class AppendData extends BaseMessage implements Serializable {
    private String key;
    private Database.Data data;

    public AppendData() {
        super(APPEND_DATA);
    }

    public AppendData(Data request){
        super(APPEND_DATA);
        this.key = request.getKey();
        this.data = new Database.Data( request.getData() );
    }

    public AppendData(String key, Database.Data data) {
        super(APPEND_DATA);
        this.key = key;
        this.data = data;
    }

}
