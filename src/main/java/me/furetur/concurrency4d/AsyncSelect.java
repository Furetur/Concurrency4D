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
            return Optional.of(aMsg.map(Either.Left::new));
        } else {
            var b = bChannel.tryReceive();
            return b.map(bMsg -> bMsg.map(Either.Right::new));
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
            if (either instanceof Either.Left<A,B> l) {
                aChannel.unreceive(Message.value(l.value()));
            } else if (either instanceof Either.Right<A,B> r) {
                bChannel.unreceive(Message.value(r.value()));
            }
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
