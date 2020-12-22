package com.company;

import java.io.Serializable;

public class BaseMessage implements Serializable {
     private final TYPE type;

    public BaseMessage(TYPE type) {
        this.type = type;
    }

    public TYPE getType() {
        return type;
    }

    public enum TYPE{
        NEW_LEADER,
        WHO_IS_LEADER
    }

    @Override
    public String toString() {
        return "BaseMessage{" +
                "type=" + type +
                '}';
    }
}
