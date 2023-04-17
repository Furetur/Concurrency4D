package me.furetur.concurrency4d;

import me.furetur.concurrency4d.data.Either;

public interface AsyncGraph extends Graph {
    static AsyncGraph create() {
        return new AsyncGraphImpl();
    }

    <A, B> AsyncReceiveChannel<Either<A, B>> select(ReceiveChannel<A> a, ReceiveChannel<B> b);
    <T> AsyncReceiveChannel<T> toAsync(ReceiveChannel<T> channel);
}
