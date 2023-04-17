package me.furetur.concurrency4d;

interface InternalChannel<T> extends InternalSendChannel<T>, InternalReceiveChannel<T>, Channel<T> {
}
