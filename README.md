
# 多个线程有序并发执行，支持连贯操作
## 前言
在进行分布式RPC调用时是比较耗时的，如果能让多个RPC并发运行可大大节约运行时间。但是由于业务逻辑复杂多变，多个RPC之间还可能存在依赖关系，例如某个RPC需要先执行，剩下的才能继续执行。
如果能控制RPC执行顺序，再结合多线程并发，那就既能保证业务逻辑，又能大大节省调用时间。

## 实现
由于要执行的代码内容是不确定的，所以采用lamda表达式来接收要执行的内容(Action)，回调(Callback)以及异常处理(Error)，要接收lamda表达式，需要先创建三个接口。

Action接口:
```java
package com.perez.common.async;

public interface Action<R> {
    /**
     * 需要执行的方法
     * @return
     */
    public R action();
}
```
Callback接口：
```java
package com.perez.common.async;

public interface Callback<R> {
    /**
     * 回调方法
     * @param result action接口的返回值
     */
    public void callback(R result);
}
```

Error接口：
```java
package com.jrender.common.async;

public interface Error {
    /**
     * 线程异常时执行的方法
     */
    public void error();
}
```
**Caller类**用于存储每个RPC的执行内容，回调以及异常处理，同时利用threadIndex字段记录该RPC操作的顺序：
```java
package com.perez.common.async;

public class Caller implements Comparable<Caller> {
    private Action action;
    private Callback callback;
    private Error error;
    private int threadIndex;

    public Caller(Action action,Callback callback, Error error, int threadIndex) {
        this.action = action;
        this.callback = callback;
        this.error = error;
        this.threadIndex = threadIndex;
    }

    public Action getAction() {
        return action;
    }

    public Callback getCallback() {
        return callback;
    }

    public Error getError() {
        return error;
    }

    public int getThreadIndex() {
        return threadIndex;
    }

    @Override
    public int compareTo(Caller o) {
        return this.getThreadIndex()-o.getThreadIndex();
    }
}
```
**AsyncStack类**是具体的业务逻辑，通过创建Caller类的实例保存RPC信息并将其存入callerList队列中，然后通过threadIndex来依次执行线程，threadIndex的获取方式如下：
```java
1、*构造方法*创建新的Caller类实例的threadIndex为0；
2、*sync()方法*创建新的Caller类实例，threadIndex不变;
3、*before()方法*创建新的Caller类实例，threadIndex-1;
4、*after()方法*创建新的Caller类实例，threadIndex+1;
5、*beforeBack()方法*先执行threadIndex-1，再创建Caller类实例，然后threadIndex+1;
6、*afterBack()方法*先执行threadIndex+1，再创建Caller类实例，然后threadIndex-1;
```
Caller列表的执行逻辑请参考代码和注释：
```java
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
```
## 使用方法:
     new AsyncStack(()->线程1要执行的方法,(result)->线程1要执行的回调,()->线程1要执行的异常处理)
      .sync(() -> 线程2要执行的方法, (result) -> 线程2要执行的回调,()->线程2要执行的异常处理)
      .after(() -> 线程3要执行的方法, (result) -> 线程3要执行的回调,()->线程3要执行的异常处理)
      .after(() -> 线程4要执行的方法, (result) -> 线程4要执行的回调,()->线程4要执行的异常处理)
      .sync(() -> 线程5要执行的方法, (result) -> 线程5要执行的回调,()->线程5要执行的异常处理)
      .start();
如上:线程1和线程2将同步执行；线程1和2执行完后，线程3才能执行；线程3执行完后，线程4和线程5将同步执行。
