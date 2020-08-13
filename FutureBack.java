package com.jrender.common.async;

import java.util.concurrent.FutureTask;

public class FutureBack {
    private FutureTask futureTask;
    private Callback callback;
    private Error error;

    public FutureBack(FutureTask futureTask, Callback callback,Error error) {
        this.futureTask = futureTask;
        this.callback = callback;
        this.error = error;
    }

    public FutureTask getFutureTask() {
        return futureTask;
    }

    public Callback getCallback() {
        return callback;
    }

    public Error getError() {
        return error;
    }
}
