package com.github.furetur.concurrency4d;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;


class ThreadInfo {
    private final static ThreadLocal<ThreadInfo> threadInfo = ThreadLocal.withInitial(() -> new ThreadInfo(Thread.currentThread()));

    private final Thread thread;
    private final AtomicBoolean permit = new AtomicBoolean(false);

    private static final Log log = new Log(ThreadInfo.class);

    ThreadInfo(Thread thread) {
        this.thread = thread;
    }

    static ThreadInfo currentThreadInfo() {
        return threadInfo.get();
    }

    static void park(Object blocker) {
        var t = threadInfo.get();
        while (!t.eatPermit()) {
            log.debug("parking, waiting for permit");
            LockSupport.park(blocker);
        }
        log.debug("permit received, unparking");
    }

    void unpark() {
        log.debug(() -> "unparking:\n" + printThreadFullState());
        permit.set(true);
        LockSupport.unpark(thread);
    }

    private String printThreadFullState() {
        var s = new StringBuilder();
        s.append("\tTHREAD: ").append(thread).append("\n");
        s.append("\tSTATE: ").append(thread.getState()).append("\n");
        var stack = Arrays.stream(thread.getStackTrace()).map(x -> "\t\t" + x + "\n").collect(Collectors.joining());
        s.append(stack);
        return s.toString();
    }

    private boolean eatPermit() {
        return permit.compareAndSet(true, false);
    }
}
