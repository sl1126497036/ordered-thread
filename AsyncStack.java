package com.jrender.common.async;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * @author SL
 * @version 1.0
 * @date 2020/3/22 3:19 下午
 * 有序线程。通过lambda接收需要执行的方法、回调和异常处理并进行排序，按顺序新建线程执行，序号一致的同步执行
 */
public class AsyncStack {
    private int threadIndex = 0;
    private List<Caller> callerList = new LinkedList<>();

    public AsyncStack(Action action) {
        push(action, null, null);
    }

    public AsyncStack(Action action, Callback callback) {
        push(action, callback, null);
    }
    public AsyncStack(Action action, Callback callback, Error error) {
        push(action, callback, error);
    }

    /**
     * 将待执行方法加入列表
     * @param action
     * @param callback
     * @param error
     */
    private void push(Action action, Callback callback, Error error) {
        callerList.add(new Caller(action, callback, error, threadIndex));
    }

    /**
     * 同步执行
     * @param action
     * @return
     */
    public AsyncStack sync(Action action) {
        return sync(action, null, null);
    }

    /**
     * 同步执行
     * @param action
     * @param callback
     * @return
     */
    public AsyncStack sync(Action action, Callback callback) {
        return sync(action, callback, null);
    }

    /**
     * 同步执行
     * @param action
     * @param callback
     * @param error
     * @return
     */
    public AsyncStack sync(Action action, Callback callback, Error error) {
        push(action, callback, error);
        return this;
    }

    /**
     * 在上一个线程之前执行
     * @param action
     * @return
     */
    public AsyncStack before(Action action) {
        return before(action, null, null);
    }

    /**
     * 在上一个线程之前执行
     * @param action
     * @param callback
     * @return
     */
    public AsyncStack before(Action action, Callback callback) {
        return before(action, callback, null);
    }

    /**
     * 在上一个线程之前执行
     * @param action
     * @param callback
     * @param error
     * @return
     */
    public AsyncStack before(Action action, Callback callback, Error error) {
        threadIndex--;
        push(action, callback, error);
        return this;
    }

    /**
     * 在上一个线程之后执行
     * @param action
     * @return
     */
    public AsyncStack after(Action action) {
        return after(action, null, null);
    }

    /**
     * 在上一个线程之后执行
     * @param action
     * @param callback
     * @return
     */
    public AsyncStack after(Action action, Callback callback) {
        return after(action, callback, null);
    }

    /**
     * 在上一个线程之后执行
     * @param action
     * @param callback
     * @param error
     * @return
     */
    public AsyncStack after(Action action, Callback callback, Error error) {
        threadIndex++;
        push(action, callback, error);
        return this;
    }

    /**
     * 在上一个线程之前执行，并将序号回退
     * @param action
     * @return
     */
    public AsyncStack beforeBack(Action action) {
        return beforeBack(action, null, null);
    }
    /**
     * 在上一个线程之前执行，并将序号回退
     * @param action
     * @param callback
     * @return
     */
    public AsyncStack beforeBack(Action action, Callback callback) {
        return beforeBack(action, callback, null);
    }
    /**
     * 在上一个线程之前执行，并将序号回退
     * @param action
     * @param callback
     * @param error
     * @return
     */
    public AsyncStack beforeBack(Action action, Callback callback, Error error) {
        threadIndex--;
        push(action, callback, error);
        threadIndex++;
        return this;
    }

    /**
     * 在上一个线程之后执行，并将序号回退
     * @param action
     * @return
     */
    public AsyncStack afterBack(Action action) {
        return afterBack(action);
    }

    /**
     * 在上一个线程之后执行，并将序号回退
     * @param action
     * @param callback
     * @return
     */
    public AsyncStack afterBack(Action action, Callback callback) {
        return afterBack(action, callback);
    }

    /**
     * 在上一个线程之后执行，并将序号回退
     * @param action
     * @param callback
     * @param error
     * @return
     */
    public AsyncStack afterBack(Action action, Callback callback, Error error) {
        threadIndex++;
        push(action, callback, error);
        threadIndex--;
        return this;
    }

    /**
     * 开始执行
     */
    public void start() {
        int preIndex = callerList.get(0).getThreadIndex();
        Collections.sort(callerList);
        List<Caller> syncTaskList = new ArrayList<>();
        for (Caller caller : callerList) {
            if (preIndex != caller.getThreadIndex()) {
                doTask(syncTaskList);
                syncTaskList.clear();
                preIndex = caller.getThreadIndex();
            }
            syncTaskList.add(caller);
        }
        doTask(syncTaskList);
    }

    private void doTask(List<Caller> callerList) {
        if (null != callerList && callerList.size() > 0) {
            CountDownLatch latch = new CountDownLatch(1);
            ExecutorService exec = Executors.newFixedThreadPool(callerList.size());
            List<FutureBack> futureBackList = new ArrayList<>();
            for (Caller caller : callerList) {
                FutureTask futureTask = new FutureTask(() -> {
                    latch.await();
                    return caller.getAction().action();//执行action
                });
                exec.execute(futureTask);
                futureBackList.add(new FutureBack(futureTask, caller.getCallback(), caller.getError()));
            }
            latch.countDown();
            for (FutureBack futureBack : futureBackList) {
                FutureTask futureTask = futureBack.getFutureTask();
                try {
                    Object object = futureTask.get();
                    if (null != futureBack.getCallback()) {
                        futureBack.getCallback().callback(object);//执行callback
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (null != futureBack.getCallback()) {
                        futureBack.getError().error();//执行error
                    }
                }
            }
            exec.shutdown();
        }
    }

}
