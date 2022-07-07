package com.perez.async;

public interface Callback<R> {
    /**
     * 回调方法
     * @param result action接口的返回值
     */
    void callback(R result);
}
