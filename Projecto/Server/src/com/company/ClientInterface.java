package com.company;

public interface ClientInterface {

    Database.Data read(String key);

    void write(String key, Database.Data data);
}
