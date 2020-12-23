package com.company.messages;

import com.company.Database;
import rpcsclientstubs.Data;

import java.io.Serializable;

import static com.company.messages.BaseMessage.TYPE.APPEND_DATA;

public class AppendData extends BaseMessage implements Serializable {
    private final String key;
    private final Database.Data data;

    public AppendData(String key, Database.Data data) {
        super(APPEND_DATA);
        this.key = key;
        this.data = data;
    }

    public AppendData(Data request){
        super(APPEND_DATA);
        this.key = request.getKey();
        this.data = new Database.Data( request.getData() );
    }

    public String getKey() {
        return key;
    }

    public Database.Data getData() {
        return data;
    }

    @Override
    public String toString() {
        return "AppendData{" +
                "key='" + key + '\'' +
                ", data=" + data +
                '}';
    }
}
