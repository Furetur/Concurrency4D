package com.github.furetur.concurrency4d;

public interface ReceiveChannel<T> {
    Message<T> receive();
    void cancel();
}
