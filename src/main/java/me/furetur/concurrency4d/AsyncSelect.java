package me.furetur.concurrency4d;

import me.furetur.concurrency4d.data.Either;

import java.util.Optional;

class AsyncSelect<A, B> implements InternalAsyncReceiveChannel<Either<A, B>> {

    private final InternalAsyncReceiveChannel<A> aChannel;
    private final InternalAsyncReceiveChannel<B> bChannel;

    AsyncSelect(InternalAsyncReceiveChannel<A> aChannel, InternalAsyncReceiveChannel<B> bChannel) {
        this.aChannel = aChannel;
        this.bChannel = bChannel;
    }

    @Override
    public long id() {
        return -1;
    }

    @Override
    public Optional<Message<Either<A, B>>> tryReceive() {
        var a = aChannel.tryReceive();
        if (a.isPresent()) {
            var aMsg = a.get();
            if (aMsg.isValue()) {
                // if A is actually a value, we instantly return it
                return Optional.of(Message.value(new Either.Left<>(aMsg.value())));
            }
            // A is CLOSE
            // unreceive to run assertions
            aChannel.unreceive(aMsg);
            var b = bChannel.tryReceive();
            // select (close, null) = null
            // select (close, close) = close
            return b.map(bMsg -> bMsg.map(Either.Right::new));
        } else {
            // A is null. We decide based only on B
            // However, if B is CLOSE it should not be returned
            // select (null, close) = null
            var b = bChannel.tryReceive();
            if (b.isPresent()) {
                var bMsg = b.get();
                if (bMsg.isValue()) {
                    return Optional.of(Message.value(new Either.Right<>(bMsg.value())));
                } else {
                    // run assertions
                    bChannel.unreceive(bMsg);
                    // see above
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public Message<Either<A, B>> receive() {
        return Utils.keepTryReceiving(this);
    }

    @Override
    public void unreceive(Message<Either<A, B>> message) {
        if (message.isValue()) {
            var either = message.value();
            if (either instanceof Either.Left<A, B> l) {
                aChannel.unreceive(Message.value(l.value()));
            } else {
                var r = (Either.Right<A, B>) either;
                bChannel.unreceive(Message.value(r.value()));
            }
        } else {
            // select is closed iff both inputs are closed
            // this should execute a couple of assertions
            aChannel.unreceive(Message.close());
            bChannel.unreceive(Message.close());
        }
    }

    @Override
    public void registerReceiver(AsyncCoroutine coroutine) {
        aChannel.registerReceiver(coroutine);
        bChannel.registerReceiver(coroutine);
    }

    @Override
    public void cancel() {
        aChannel.cancel();
        bChannel.cancel();
    }
}
