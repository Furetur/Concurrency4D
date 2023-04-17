package me.furetur.concurrency4d;

import me.furetur.concurrency4d.data.Pair;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

class Utils {
    private static final AtomicLong nextId = new AtomicLong(0);

    static <T> Message<T> keepTryReceiving(InternalAsyncReceiveChannel<T> channel) {
        var msg = channel.tryReceive();
        while (msg.isEmpty()) {
            System.out.println("transaction: parking on " + channel + ", thread: " + Thread.currentThread());
            LockSupport.park();
            msg = channel.tryReceive();
        }
        System.out.println("transaction: tryReceive succeeded, no need to park on" + channel);
        return msg.get();
    }

    static long nextId() {
        return nextId.getAndIncrement();
    }

    static <A, B> Message<Pair<A, B>> joinMessages(Message<A> a, Message<B> b) {
        if (a.isValue() && b.isValue()) {
            return Message.value(new Pair<>(a.value(), b.value()));
        } else {
            return Message.close();
        }
    }
}
