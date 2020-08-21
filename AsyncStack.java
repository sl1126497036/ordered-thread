package com.perez.common.async;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

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
	 	Collections.sort(callerList);
        int preIndex = callerList.get(0).getThreadIndex();//preIndex表示上一个执行的线程序号
        List<Caller> syncTaskList = new ArrayList<>();
        for (Caller caller : callerList) {
            if (preIndex != caller.getThreadIndex()) {//如果当前RPC的线程序号与preIndex不同，则直接执行，并清空syncTaskList
                doTask(syncTaskList);
                syncTaskList.clear();
                preIndex = caller.getThreadIndex();//更新上一个执行的RPC线程序号
            }
            syncTaskList.add(caller);//将当前要执行的RPC加入到syncTaskList中
        }
        doTask(syncTaskList);//执行需要并发的RPC
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
