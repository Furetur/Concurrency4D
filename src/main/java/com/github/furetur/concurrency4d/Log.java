package com.github.furetur.concurrency4d;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Supplier;

@CoverageExcludeGenerated
public class Log {
    private static final boolean IS_DEBUG = System.getenv().getOrDefault("DEBUG", "false").equals("true");
    private static final LinkedBlockingDeque<String> log = new LinkedBlockingDeque<>();

    private final Object caller;

    public Log(Object caller) {
        this.caller = caller;
    }

    @CoverageExcludeGenerated
    public void debug(Supplier<String> message) {
        if (IS_DEBUG) {
            log.add("{{ " + Thread.currentThread() + " at " + caller + "}} -- " + message.get());
        }
    }

    @CoverageExcludeGenerated
    public void debug(String message) {
        debug(() -> message);
    }

    @CoverageExcludeGenerated
    public static void flush() {
        if (IS_DEBUG) {
            while (!log.isEmpty()) {
                System.out.println(log.poll());
            }
        }
    }
}
