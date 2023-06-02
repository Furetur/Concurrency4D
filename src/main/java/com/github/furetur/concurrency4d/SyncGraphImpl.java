package com.github.furetur.concurrency4d;

import com.github.furetur.concurrency4d.data.Pair;

import java.util.LinkedList;

public class SyncGraphImpl implements Graph {
    private final LinkedList<Coroutine> coroutines = new LinkedList<>();

    @Override
    public <T> Channel<T> channel(int size) {
        if (size == 1) {
            return new SyncOneChannelImpl<>();
        } else {
            return new SyncChannelImpl<>(size);
        }
    }

    @Override
    public <T> Channel<T> channel() {
        return channel(1);
    }

    @Override
    public <A, B> ReceiveChannel<Pair<A, B>> join(ReceiveChannel<A> a, ReceiveChannel<B> b) {
        return new SyncJoin<>((InternalReceiveChannel<A>) a, (InternalReceiveChannel<B>) b);
    }

    @Override
    public void coroutine(Coroutine coroutine) {
        coroutines.push(coroutine);
    }

    @Override
    public void build() {
        for (var coroutine : coroutines) {
            coroutine.build();
        }
    }
}
