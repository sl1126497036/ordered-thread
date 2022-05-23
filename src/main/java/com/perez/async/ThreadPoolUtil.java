package com.perez.async;

import java.util.concurrent.*;

/**
 * @author SL
 * @version 1.0
 * @date 2022/5/23 13:55
 */
public class ThreadPoolUtil<T> {
    /**
     * 根据cpu的数量动态的配置核心线程数和最大线程数
     */
    public static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    /**
     * 核心线程数 = CPU核心数 + 1
     */
    public static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    /**
     * 非核心线程闲置时超时1s
     */
    public static final int KEEP_ALIVE = 1;
    /**
     * 线程池最大线程数
     */
    private static final int MAXIMUM_POOL_SIZE = 20;

    private static volatile ThreadPoolUtil instance;

    private ThreadPoolExecutor executor;

    public synchronized static ThreadPoolUtil getsInstance() {
        if (instance == null) {
            instance = new ThreadPoolUtil();
        }
        return instance;
    }

    private void createExecutor(){
        executor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
                KEEP_ALIVE, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    }

    public void execute(Runnable runnable) throws RejectedExecutionException{
        if (null == executor) {
           this.createExecutor();
        }
        executor.execute(runnable);
    }

    public Future<T> submit(Callable<T> callable) throws RejectedExecutionException,NullPointerException {
        if (null == executor) {
            this.createExecutor();
        }
        return executor.submit(callable);
    }

    public void cancel(Runnable runnable) {
        if (runnable != null) {
            executor.getQueue().remove(runnable);
        }
    }
    public void shutdown(){
        executor.shutdown();
        executor = null;
    }

}
