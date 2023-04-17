package me.furetur.concurrency4d;

import java.util.Optional;

interface InternalAsyncReceiveChannel<T> extends AsyncReceiveChannel<T>, InternalReceiveChannel<T> {
    Optional<Message<T>> tryReceive();
    void unreceive(Message<T> message);
}
