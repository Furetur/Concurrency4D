package me.furetur.concurrency4d;

public interface SendChannel<T> {
    boolean send(T value);
    boolean close();
}
