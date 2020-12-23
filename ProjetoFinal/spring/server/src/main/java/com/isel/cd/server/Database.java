package com.isel.cd.server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Database {

    public final Map<String, Data> database = new HashMap<>();

    public void printDatabase(){
        StringBuilder stringBuilder = new StringBuilder("Database has " + database.size() + " entries.\n");
        database.forEach((key, value) -> stringBuilder.append("key: " + key + ", value: " + value + "\n"));
        System.out.println(stringBuilder.toString());
    }

    @lombok.Data
    public static class Data implements Serializable {
        private String data;

        public Data(String data) {
            this.data = data;
        }
    }
}
