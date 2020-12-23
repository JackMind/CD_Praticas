package com.company;

public class LeaderThread implements Runnable {
    private final String threadName;
    private Thread thread;
    private volatile boolean suspend = false;

    public LeaderThread(String serverName) {
        this.threadName = "LeaderThread-" + serverName;
    }

    @Override
    public void run() {
        try{
            while (true){


                synchronized (this) {
                    while (suspend) {
                        wait();
                    }
                }

            }
        }catch (InterruptedException e) {
            System.out.println("Thread " +  threadName + " interrupted.");
        }
    }

    public void start(){
        if(thread == null){
            thread = new Thread(this, this.threadName);
            thread.start();
        }
    }

    public void suspend() {
        suspend = true;
    }

    public synchronized void resume(){
        suspend = false;
        notify();
    }


}
