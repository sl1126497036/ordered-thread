package com.perez.async;

public interface Action<R> {
    /**
     * 需要执行的方法
     * @return
     */
    public R action();
}
