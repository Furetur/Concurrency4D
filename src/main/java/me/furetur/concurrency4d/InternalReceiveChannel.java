package me.furetur.concurrency4d;

interface InternalReceiveChannel<T> extends ReceiveChannel<T> {
    long id();
    void registerReceiver(AsyncCoroutine coroutine);
    Message<T> receive();
    void cancel();

}
