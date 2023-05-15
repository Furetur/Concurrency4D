package com.github.furetur.concurrency4d;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AsyncCoroutine {
    private final List<ReceiveChannel<?>> inputs;
    private final List<SendChannel<?>> outputs;

    private final AtomicBoolean isAlive = new AtomicBoolean(false);
    private final AtomicReference<ThreadInfo> threadInfo = new AtomicReference<>();

    private static final ThreadFactory threadFactory = Thread.ofVirtual().factory();

    private final Log log = new Log(this);

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
                threadInfo.set(ThreadInfo.currentThreadInfo());
                this.run();
                log.debug("Coroutine returned");
            });
            t.setName(this.toString());
            t.start();
        } else {
            var t = threadInfo.get();
            if (t != null) {
                // the coroutine is ALIVE
                log.debug("Sending unpark");
                t.unpark();
            }
            // if t == null then the coroutine is STARTING
        }
    }

    protected abstract void run();
}
