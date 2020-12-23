package com.consensus.test;

public interface ClientInterface {

    TestApplication.Value read(int key);

    void write(int key, TestApplication.Value value);
}
