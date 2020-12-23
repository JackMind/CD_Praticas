package com.isel.cd.server.messages;


import com.isel.cd.server.DataEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class ConsensusVoting extends BaseMessage implements Serializable {
    private UUID transactionId;
    private String key;
    private DataEntity.Data data;

    public ConsensusVoting() {
        super(TYPE.CONSENSUS_VOTING);
    }

    public ConsensusVoting(UUID transactionId, String key, DataEntity.Data data) {
        super(TYPE.CONSENSUS_VOTING);
        this.transactionId = transactionId;
        this.key = key;
        this.data = data;
    }

}
