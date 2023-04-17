package me.furetur.concurrency4d;

interface InternalAsyncChannel<T> extends InternalAsyncReceiveChannel<T>, InternalAsyncSendChannel<T>, InternalChannel<T>, AsyncChannel<T> {
}
