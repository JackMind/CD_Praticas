package com.company.messages;

import com.company.Database;
import rpcsclientstubs.Data;

import java.io.Serializable;
import java.util.UUID;

public class ConsensusVoting extends BaseMessage implements Serializable {
    private final UUID transactionId;
    private final String key;
    private final Database.Data data;

    public ConsensusVoting(UUID transactionId, String key, Database.Data data) {
        super(TYPE.CONSENSUS_VOTING);
        this.transactionId = transactionId;
        this.key = key;
        this.data = data;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public Database.Data getData() {
        return data;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "ConsensusVoting{" +
                "transactionId=" + transactionId +
                ", key='" + key + '\'' +
                ", data=" + data +
                '}';
    }
}
