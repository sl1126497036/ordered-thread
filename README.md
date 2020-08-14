# ordered-thread
用于RPC等耗时操作，使多个线程有序并发，支持连贯操作，如有问题请指出。
### 使用方法:
     new AsyncStack(()->线程1要执行的方法,(result)->线程1要执行的回调)
      .sync(() -> 线程2要执行的方法, (result) -> 线程2要执行的回调)
      .after(() -> 线程3要执行的方法, (result) -> 线程3要执行的回调)
      .after(() -> 线程4要执行的方法, (result) -> 线程4要执行的回调)
      .sync(() -> 线程5要执行的方法, (result) -> 线程5要执行的回调)
      .start(); 
      
    如上:线程1和线程2将同步执行；线程1和2执行完后，线程3才能执行；线程3执行完后，线程4和线程5将同步执行。
    
### 执行过程：
**Caller类** 用于保存要执行的方法(action)、回调(callback)、异常处理(error)和线程执行顺序(threadIndex)。

**AsyncStack类** 将创建Caller类的实例并存入callerList队列中，其过程如下：

    构造方法创建新的Caller类实例的threadIndex为0；
    sync()方法创建新的Caller类实例，threadIndex不变;
    before()方法创建新的Caller类实例，threadIndex-1;
    after()方法创建新的Caller类实例，threadIndex+1;
    beforeBack()方法先执行threadIndex-1，再创建Caller类实例，然后threadIndex+1;
    afterBack()方法先执行threadIndex+1，再创建Caller类实例，然后threadIndex-1;
