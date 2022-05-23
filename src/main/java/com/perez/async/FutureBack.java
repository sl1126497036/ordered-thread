package com.perez.async;

import java.util.concurrent.Future;

public class FutureBack {
    private Future future;
    private Callback callback;
    private Error error;
    private Long timeout;

    public FutureBack(Future future, Callback callback, Error error,Long timeout) {
        this.future = future;
        this.callback = callback;
        this.error = error;
        this.timeout = timeout;
    }

    public Future getFuture() {
        return future;
    }

    public Callback getCallback() {
        return callback;
    }

    public Error getError() {
        return error;
    }

    public Long getTimeout(){ return timeout;}
}
