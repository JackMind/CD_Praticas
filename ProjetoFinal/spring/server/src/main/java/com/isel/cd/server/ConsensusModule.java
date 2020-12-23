package com.isel.cd.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConsensusModule {
    private final Map<UUID,Voting> activeVoting = new ConcurrentHashMap<>();


    public UUID submitVote(final int expectedVotes, final VoteCompletedCallBack callBack){
        UUID transactionId;
        do{
            transactionId = UUID.randomUUID();
        } while (activeVoting.containsKey(transactionId));

        activeVoting.put(transactionId, new Voting(expectedVotes, callBack));

        return transactionId;
    }

    public void appendVote(UUID transactionId, boolean vote){
        Voting appendedVote = activeVoting.get(transactionId).appendVote();
        if(!vote){
            appendedVote.setResult(false);
            appendedVote.getCallBack().completed(true);
        } else if(appendedVote.getExpectedVotes() == appendedVote.getVotesReceived()){
            System.out.println("Call callback complete!");
            appendedVote.setResult(true);
            activeVoting.put(transactionId, appendedVote);
            appendedVote.getCallBack().completed(true);
        }else{
            activeVoting.put(transactionId, appendedVote);
        }
    }

    public boolean getResultAndRemove(UUID transactionId){
        boolean result = this.activeVoting.get(transactionId).isResult();
        this.activeVoting.remove(transactionId);
        return result;
    }

    public static class Voting{
        private final int expectedVotes;
        private final VoteCompletedCallBack callBack;
        private int votesReceived;
        private boolean result = false;

        public Voting(int expectedVotes, final VoteCompletedCallBack callBack) {
            this.expectedVotes = expectedVotes;
            this.callBack = callBack;
            this.votesReceived = 1; //Self-vote
        }

        public VoteCompletedCallBack getCallBack() {
            return callBack;
        }

        public Voting appendVote(){
            this.votesReceived++;
            return this;
        }

        public int getExpectedVotes() {
            return expectedVotes;
        }

        public int getVotesReceived() {
            return votesReceived;
        }

        public boolean isResult() {
            return result;
        }

        public void setResult(boolean result) {
            this.result = result;
        }
    }

    public interface VoteCompletedCallBack {
        void completed(boolean completed);
    }
}
