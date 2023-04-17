package me.furetur.concurrency4d;

public interface Channel<T> extends ReceiveChannel<T>, SendChannel<T> {
}
