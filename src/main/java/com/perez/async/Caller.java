package com.perez.async;


public class Caller implements Comparable<Caller> {
    private Action action;
    private Callback callback;
    private Error error;
    private int threadIndex;
    private Long timeout;

    public Caller(Action action, Callback callback, Error error, int threadIndex,Long timeout) {
        this.action = action;
        this.callback = callback;
        this.error = error;
        this.threadIndex = threadIndex;
        this.timeout = timeout;
    }

    public Action getAction() {
        return action;
    }

    public Callback getCallback() {
        return callback;
    }

    public Error getError() {
        return error;
    }

    public int getThreadIndex() {
        return threadIndex;
    }

    public Long getTimeout(){return timeout;}

    @Override
    public int compareTo(Caller o) {
        return this.getThreadIndex() - o.getThreadIndex();
    }
}
