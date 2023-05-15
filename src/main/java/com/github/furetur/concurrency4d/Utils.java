package com.github.furetur.concurrency4d;

import com.github.furetur.concurrency4d.data.Pair;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

class Utils {
    private static final AtomicLong nextId = new AtomicLong(0);

    private static final Log log = new Log(Utils.class);

    static <T> Message<T> keepTryReceiving(InternalAsyncReceiveChannel<T> channel) {
        log.debug("TRANSACTION receive() START");
        log.debug("TRANSACTION ATTEMPT");
        var msg = channel.tryReceive();
        while (msg.isEmpty()) {
            log.debug(() -> "TRANSACTION parking on " + channel);
            ThreadInfo.park(channel);
            log.debug("TRANSACTION ATTEMPT");
            msg = channel.tryReceive();
        }
        Optional<Message<T>> finalMsg = msg;
        log.debug(() -> "TRANSACTION receive() END: " + finalMsg);
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
