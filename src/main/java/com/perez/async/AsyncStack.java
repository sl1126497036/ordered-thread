package com.perez.async;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class AsyncStack {
    private static final long defaultTimeout = 5000;
    private ThreadPoolUtil threadPool = ThreadPoolUtil.getsInstance();
    private int threadIndex = 0;
    private List<Caller> callerList = new LinkedList<>();

    public AsyncStack(Action action) {
        push(action, null, null, null);
    }

    public AsyncStack(Action action, Callback callback, Long timeout) {
        push(action, callback, null, timeout);
    }

    public AsyncStack(Action action, Callback callback, Error error) {
        push(action, callback, error, null);
    }

    public AsyncStack(Action action, Callback callback, Error error, Long timeout) {
        push(action, callback, error, timeout);
    }

    /**
     * 将待执行方法加入列表
     *
     * @param action
     * @param callback
     * @param error
     */
    private void push(Action action, Callback callback, Error error, Long timeout) {
        callerList.add(new Caller(action, callback, error, threadIndex, timeout));
    }

    /**
     * 同步执行
     *
     * @param action
     * @return
     */
    public AsyncStack sync(Action action) {
        return sync(action, null, null);
    }


    /**
     * 同步执行
     *
     * @param action
     * @param callback
     * @return
     */
    public AsyncStack sync(Action action, Callback callback) {
        return sync(action, callback, null);
    }

    /**
     * 同步执行
     *
     * @param action
     * @param callback
     * @return
     */
    public AsyncStack sync(Action action, Callback callback, Error error) {
        return sync(action, callback, error, null);
    }

    /**
     * 同步执行
     *
     * @param action
     * @param callback
     * @param error
     * @return
     */
    public AsyncStack sync(Action action, Callback callback, Error error, Long timeout) {
        push(action, callback, error, timeout);
        return this;
    }

    /**
     * 在上一个线程之前执行
     *
     * @param action
     * @return
     */
    public AsyncStack before(Action action) {
        return before(action, null, null);
    }

    /**
     * 在上一个线程之前执行
     *
     * @param action
     * @param callback
     * @return
     */
    public AsyncStack before(Action action, Callback callback) {
        return before(action, callback, null);
    }

    public AsyncStack before(Action action, Callback callback, Error error) {
        return before(action, callback, error, null);
    }

    /**
     * 在上一个线程之前执行
     *
     * @param action
     * @param callback
     * @param error
     * @return
     */
    public AsyncStack before(Action action, Callback callback, Error error, Long timeout) {
        threadIndex--;
        push(action, callback, error, timeout);
        return this;
    }

    /**
     * 在上一个线程之后执行
     *
     * @param action
     * @return
     */
    public AsyncStack after(Action action) {
        return after(action, null, null);
    }

    /**
     * 在上一个线程之后执行
     *
     * @param action
     * @param callback
     * @return
     */
    public AsyncStack after(Action action, Callback callback) {
        return after(action, callback, null);
    }

    public AsyncStack after(Action action, Callback callback, Error error) {
        return after(action, callback, error, null);
    }

    /**
     * 在上一个线程之后执行
     *
     * @param action
     * @param callback
     * @param error
     * @return
     */
    public AsyncStack after(Action action, Callback callback, Error error, Long timeout) {
        threadIndex++;
        push(action, callback, error, timeout);
        return this;
    }

    /**
     * 在上一个线程之前执行，并将序号回退
     *
     * @param action
     * @return
     */
    public AsyncStack beforeBack(Action action) {
        return beforeBack(action, null, null);
    }

    /**
     * 在上一个线程之前执行，并将序号回退
     *
     * @param action
     * @param callback
     * @return
     */
    public AsyncStack beforeBack(Action action, Callback callback) {
        return beforeBack(action, callback, null);
    }

    public AsyncStack beforeBack(Action action, Callback callback, Error error) {
        return beforeBack(action, callback, error, null);
    }

    /**
     * 在上一个线程之前执行，并将序号回退
     *
     * @param action
     * @param callback
     * @param error
     * @return
     */
    public AsyncStack beforeBack(Action action, Callback callback, Error error, Long timeout) {
        threadIndex--;
        push(action, callback, error, timeout);
        threadIndex++;
        return this;
    }

    /**
     * 在上一个线程之后执行，并将序号回退
     *
     * @param action
     * @return
     */
    public AsyncStack afterBack(Action action) {
        return afterBack(action);
    }

    /**
     * 在上一个线程之后执行，并将序号回退
     *
     * @param action
     * @param callback
     * @return
     */
    public AsyncStack afterBack(Action action, Callback callback) {
        return afterBack(action, callback);
    }

    public AsyncStack afterBack(Action action, Callback callback, Error error) {
        return afterBack(action, callback, error, null);
    }

    /**
     * 在上一个线程之后执行，并将序号回退
     *
     * @param action
     * @param callback
     * @param error
     * @return
     */
    public AsyncStack afterBack(Action action, Callback callback, Error error, Long timeout) {
        threadIndex++;
        push(action, callback, error, timeout);
        threadIndex--;
        return this;
    }

    /**
     * 开始执行
     */
    public void start() throws Exception {
        Collections.sort(callerList);
        //preIndex表示上一个执行的线程序号
        int preIndex = callerList.get(0).getThreadIndex();
        List<Caller> syncTaskList = new ArrayList<>();
        for (Caller caller : callerList) {
            //如果当前RPC的线程序号与preIndex不同，则直接执行，并清空syncTaskList
            if (preIndex != caller.getThreadIndex()) {
                doTask(syncTaskList);
                syncTaskList.clear();
                //更新上一个执行的RPC线程序号
                preIndex = caller.getThreadIndex();
            }
            //将当前要执行的RPC加入到syncTaskList中
            syncTaskList.add(caller);
        }
        //执行需要并发的RPC
        doTask(syncTaskList);
    }

    private void doTask(List<Caller> callerList) throws Exception {
        if (null != callerList && callerList.size() > 0) {
            CountDownLatch latch = new CountDownLatch(1);
            List<FutureBack> futureBackList = new ArrayList<>();
            for (Caller caller : callerList) {
                try {
                    Future future = threadPool.submit(() -> {
                        latch.await();
                        //执行action
                        return caller.getAction().action();
                    });
                    futureBackList.add(new FutureBack(future, caller.getCallback(), caller.getError(), caller.getTimeout()));
                } catch (RejectedExecutionException e) {
                    //线程池已满，系统繁忙
                    throw e;
                } catch (Exception e) {
                    throw e;
                }
            }
            latch.countDown();
            for (FutureBack futureBack : futureBackList) {
                Future future = futureBack.getFuture();
                long timeout = null == futureBack.getTimeout() ? defaultTimeout : futureBack.getTimeout();
                try {
                    Object object = future.get(timeout, TimeUnit.MILLISECONDS);
                    if (null != futureBack.getCallback()) {
                        //执行callback
                        futureBack.getCallback().callback(object);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (null != futureBack.getCallback()) {
                        //执行error
                        futureBack.getError().error(e);
                    }
                }
            }
        }
    }

//    /**
//     * 获取最大同步线程数
//     *
//     * @return
//     */
//    private int maxSyncCount() {
//        Map<Integer, Integer> countMap = new HashMap<>();
//        this.callerList.forEach(item -> {
//            if (countMap.containsKey(item.getThreadIndex())) {
//                countMap.put(item.getThreadIndex(), countMap.get(item.getThreadIndex()) + 1);
//            } else {
//                countMap.put(item.getThreadIndex(), 1);
//            }
//        });
//        return countMap.values().stream().max(Comparator.naturalOrder()).orElse(1);
//    }
}
