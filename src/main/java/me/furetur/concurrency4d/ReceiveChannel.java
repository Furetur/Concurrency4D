package me.furetur.concurrency4d;

public interface ReceiveChannel<T> {
    Message<T> receive();
    void cancel();
}
