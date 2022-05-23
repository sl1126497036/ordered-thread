package com.perez.async;

public interface Error {
    /**
     * 线程异常时执行的方法
     */
    public void error(Exception e);
}
