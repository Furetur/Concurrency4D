package me.furetur.concurrency4d;

public interface AsyncChannel<T> extends AsyncReceiveChannel<T>, AsyncSendChannel<T>, Channel<T> {
}
