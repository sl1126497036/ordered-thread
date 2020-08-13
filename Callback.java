package com.jrender.common.async;

public interface Callback<R> {
    /**
     * 回调方法
     * @param result action接口的返回值
     */
    public void callback(R result);
}
