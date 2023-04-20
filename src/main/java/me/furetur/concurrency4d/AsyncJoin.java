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
        System.out.println("join tryReceive");
        var a = aChannel.tryReceive();
        var b = bChannel.tryReceive();

        if (a.isPresent() && b.isPresent()) {
            var aMsg = a.get();
            var bMsg = b.get();
            var msg = Utils.joinMessages(aMsg, bMsg);
            // if at least one of the messages is CLOSE
            if (!msg.isValue()) {
                // one of the messages may contain a value
                // we roll it back
                a.ifPresent(aChannel::unreceive);
                b.ifPresent(bChannel::unreceive);
                // and auto-cancel the channels
                aChannel.cancel();
                bChannel.cancel();
            }
            return Optional.of(msg);
        } else if (a.isPresent()){
            var aMsg = a.get();
            if (aMsg.isValue()) {
                // rollback
                aChannel.unreceive(aMsg);
            } else {
                bChannel.cancel();
                return Optional.of(Message.close());
            }
        } else if (b.isPresent()) {
            var bMsg = b.get();
            if (bMsg.isValue()) {
                // rollback
                bChannel.unreceive(bMsg);
            } else {
                aChannel.cancel();
                return Optional.of(Message.close());
            }
        }
        return Optional.empty();
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
