package me.furetur.concurrency4d;

interface InternalSendChannel<T> extends SendChannel<T> {
    void registerSender(AsyncCoroutine coroutine);
    boolean send(T value);
    boolean close();
}
