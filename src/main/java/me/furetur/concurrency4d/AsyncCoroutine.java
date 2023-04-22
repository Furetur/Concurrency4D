package me.furetur.concurrency4d;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public abstract class AsyncCoroutine {
    private final List<ReceiveChannel<?>> inputs;
    private final List<SendChannel<?>> outputs;

    private final AtomicBoolean isAlive = new AtomicBoolean(false);
    private final AtomicReference<Thread> thread = new AtomicReference<>();

    private static ThreadFactory threadFactory = Thread.ofVirtual().factory();

    private Log log = new Log(this);

    public AsyncCoroutine(List<ReceiveChannel<?>> inputs, List<SendChannel<?>> outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    void build() {
        for (var in : inputs) {
            ((InternalReceiveChannel<?>) in).registerReceiver(this);
        }
        for (var out : outputs) {
            ((InternalSendChannel<?>) out).registerSender(this);
        }
    }

    void schedule() {
        var wasNotAlive = isAlive.compareAndSet(false, true);
        if (wasNotAlive) {
            // the coroutine is still NOT ALIVE
            log.debug("Starting coroutine");
            var t = threadFactory.newThread(() -> {
                thread.set(Thread.currentThread());
                this.run();
                log.debug("Coroutine returned");
            });
            t.setName(this.toString());
            t.start();
        } else {
            var t = thread.get();
            if (t != null) {
                // the coroutine is ALIVE
                log.debug("Sending unpark");
                LockSupport.unpark(t);
            }
            // if t == null then the coroutine is STARTING
        }
    }

    protected abstract void run();
}
