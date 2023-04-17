package me.furetur.concurrency4d;

import me.furetur.concurrency4d.data.Either;
import me.furetur.concurrency4d.data.Pair;

import java.util.LinkedList;
import java.util.List;

public class AsyncGraphImpl implements AsyncGraph {
    private final LinkedList<AsyncCoroutine> coroutines = new LinkedList<>();

    public <T> AsyncChannel<T> channel() {
        return new AsyncChannelImpl<>();
    }

    public <T> AsyncChannel<T> channel(int size) {
        // TODO: implement sized channels
        return channel();
    }

    public <A, B> AsyncReceiveChannel<Pair<A, B>> join(ReceiveChannel<A> a, ReceiveChannel<B> b) {
        var aAsync = a instanceof AsyncReceiveChannel<A>;
        var bAsync = b instanceof AsyncReceiveChannel<B>;

        if (!aAsync && !bAsync) {
            var join = new SyncJoin<>((InternalReceiveChannel<A>) a, (InternalReceiveChannel<B>) b);
            return toAsync(join);
        } else if (aAsync && bAsync) {
            return new AsyncJoin<>((InternalAsyncReceiveChannel<A>) a, (InternalAsyncReceiveChannel<B>) b);
        } else if (!aAsync) {
            return new AsyncJoin<>((InternalAsyncReceiveChannel<A>) toAsync(a), (InternalAsyncReceiveChannel<B>) b);
        } else {
            return new AsyncJoin<>((InternalAsyncReceiveChannel<A>) a, (InternalAsyncReceiveChannel<B>) toAsync(b));
        }
    }

    public <A, B> AsyncReceiveChannel<Either<A, B>> select(ReceiveChannel<A> a, ReceiveChannel<B> b) {
        if (!(a instanceof AsyncReceiveChannel<A>)) a = toAsync(a);
        if (!(b instanceof AsyncReceiveChannel<B>)) b = toAsync(b);
        return new AsyncSelect<>((InternalAsyncReceiveChannel<A>) a, (InternalAsyncReceiveChannel<B>) b);
    }

    public void coroutine(AsyncCoroutine coroutine) {
        coroutines.push(coroutine);
    }

    @Override
    public void coroutine(Coroutine coroutine) {
        coroutine((AsyncCoroutine) coroutine);
    }

    public <T> AsyncReceiveChannel<T> toAsync(ReceiveChannel<T> channel) {
        var chan = this.<T>channel();
        coroutine(new CoId<>(channel, chan));
        return chan;
    }

    public void build() {
        for (var coroutine : coroutines) {
            coroutine.build();
        }
    }

    private static class CoId<T> extends Coroutine {
        private final ReceiveChannel<T> in;
        private final SendChannel<T> out;

        CoId(ReceiveChannel<T> in, SendChannel<T> out) {
            super(List.of(in), List.of(out));
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            var msg = in.receive();
            while (msg.isValue()) {
                out.send(msg.value());
            }
            out.close();
        }
    }
}
