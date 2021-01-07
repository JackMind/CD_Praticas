package com.isel.cd.server;

public class TimeoutException extends Exception{
    public TimeoutException() {
        super("Timeout reached");
    }
}
