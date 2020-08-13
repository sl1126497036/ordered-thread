package com.jrender.common.async;


public class Caller implements Comparable<Caller> {
    private Action action;
    private Callback callback;
    private Error error;
    private int threadIndex;

    public Caller(Action action,Callback callback, Error error, int threadIndex) {
        this.action = action;
        this.callback = callback;
        this.error = error;
        this.threadIndex = threadIndex;
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

    @Override
    public int compareTo(Caller o) {
        return this.getThreadIndex()-o.getThreadIndex();
    }
}
