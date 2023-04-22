package me.furetur.concurrency4d;

import me.furetur.concurrency4d.data.Pair;

import java.util.Optional;

class AsyncJoin<A, B> implements InternalAsyncReceiveChannel<Pair<A, B>> {
    private final InternalAsyncReceiveChannel<A> aChannel;
    private final InternalAsyncReceiveChannel<B> bChannel;

    AsyncJoin(InternalAsyncReceiveChannel<A> aChannel, InternalAsyncReceiveChannel<B> bChannel) {
        this.aChannel = aChannel;
        this.bChannel = bChannel;
    }

    @Override
    public long id() {
        return -1;
    }

    @Override
    public void registerReceiver(AsyncCoroutine coroutine) {
        this.aChannel.registerReceiver(coroutine);
        this.bChannel.registerReceiver(coroutine);
    }

    @Override
    public Optional<Message<Pair<A, B>>> tryReceive() {
        return tryReceiveHelper(null, null);
    }

    private Optional<Message<Pair<A, B>>> tryReceiveHelper(Message<A> msgA, Message<B> msgB) {
        // firstly, we receive from the left channel
        if (msgA == null) {
            var a = aChannel.tryReceive();
            if (a.isPresent()) {
                return tryReceiveHelper(a.get(), null);
            } else {
                // join (null, not received)
                // we schedule the second task anyway
                var b = bChannel.tryReceive();
                b.ifPresent(bChannel::unreceive);
                return Optional.empty();
            }
        } else {
            if (msgA.isValue()) {
                if (msgB == null) {
                    // join (value, not received)
                    var b = bChannel.tryReceive();
                    if (b.isPresent()) {
                        // join (value, value)
                        return tryReceiveHelper(msgA, b.get());
                    } else {
                        // join (value, null)
                        aChannel.unreceive(msgA);
                        return Optional.empty();
                    }
                } else {
                    if (msgB.isValue()) {
                        // join (value, value)
                        return Optional.of(Message.value(new Pair<>(msgA.value(), msgB.value())));
                    } else {
                        // join (value, close)
                        aChannel.unreceive(msgA);
                        return Optional.of(Message.close());
                    }
                }
            } else {
                // join (close, ?)
                assert msgB == null;
                bChannel.cancel();
                return Optional.of(Message.close());
            }
        }
    }

    @Override
    public Message<Pair<A, B>> receive() {
        return Utils.keepTryReceiving(this);
    }

    @Override
    public void unreceive(Message<Pair<A, B>> message) {
        if (message.isValue()) {
            var a = message.value().first();
            var b = message.value().second();
            aChannel.unreceive(Message.value(a));
            bChannel.unreceive(Message.value(b));
        }
        // we actually don't know which channel is closed, so we cannot unreceive
    }

    @Override
    public void cancel() {
        aChannel.cancel();
        bChannel.cancel();
    }

    @Override
    public String toString() {
        return "Join(" + aChannel + ", " + bChannel + ")";
    }
}
