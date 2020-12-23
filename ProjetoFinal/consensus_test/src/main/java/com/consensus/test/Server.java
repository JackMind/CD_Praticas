package com.consensus.test;

import java.util.HashMap;

public class Server implements ClientInterface{
    private final HashMap<Integer, TestApplication.Value> REPO = new HashMap<>();

    @Override
    public TestApplication.Value read(int key) {
        if(REPO.containsKey(key)){
            return REPO.get(key);
        }
        //TODO: request key
        return null;
    }

    @Override
    public void write(int key, TestApplication.Value value) {
        if(REPO.containsKey(key)){
            //TODO: send new value
        }
        REPO.put(key, value);
    }


}
