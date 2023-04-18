package me.furetur.concurrency4d;

import me.furetur.concurrency4d.data.Pair;

public interface Graph {

    static Graph create() {
        return new SyncGraphImpl();
    }

    <T> Channel<T> channel(int size);

    <T> Channel<T> channel();

    <A, B> ReceiveChannel<Pair<A, B>> join(ReceiveChannel<A> a, ReceiveChannel<B> b);

    void coroutine(Coroutine coroutine);

    void build();
}
