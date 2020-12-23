package com.consensus.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

@SpringBootApplication
public class TestApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    public enum STATE{
        FOLLOWER,
        LEADER,
        CANDIDATE
    }

    @Data
    public static class Value implements Serializable {
        private String data;
    }

    private static final HashMap<Integer, Value> REPO = new HashMap<>();

    private STATE myState;
    private FollowerInterface followerInterface;
    private LeaderInterface leaderInterface;

    @Override
    public void run(String... args) throws Exception {


    }

    public interface HeartBeat {
        boolean ping(int term);
    }



}
