package me.furetur.concurrency4d;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

public abstract class AsyncCoroutine {
    private final List<ReceiveChannel<?>> inputs;
    private final List<SendChannel<?>> outputs;

    private final AtomicBoolean isAlive = new AtomicBoolean(false);
    private final AtomicReference<Thread> thread = new AtomicReference<>();

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
            new Thread(() -> {
                thread.set(Thread.currentThread());
                this.run();
            }).start();
        } else {
            var t = thread.get();
            if (t != null) {
                // the coroutine is ALIVE
                System.out.println("unparking " + this + ", thread: " + t);
                LockSupport.unpark(t);
            }
            // if t == null then the coroutine is STARTING
        }
    }

    protected abstract void run();
}
