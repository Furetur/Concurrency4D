package com.github.furetur.concurrency4d;

import com.github.furetur.concurrency4d.data.Pair;

class SyncJoin<A, B> implements InternalReceiveChannel<Pair<A, B>> {
    private final long _id;
    private final InternalReceiveChannel<A> aChannel;
    private final InternalReceiveChannel<B> bChannel;

    SyncJoin(InternalReceiveChannel<A> aChannel, InternalReceiveChannel<B> bChannel) {
        this._id = Utils.nextId();
        this.aChannel = aChannel;
        this.bChannel = bChannel;
    }

    @Override
    public long id() {
        return _id;
    }

    @Override
    public void registerReceiver(AsyncCoroutine coroutine) {
        this.aChannel.registerReceiver(coroutine);
        this.bChannel.registerReceiver(coroutine);
    }

    @Override
    public Message<Pair<A, B>> receive() {
        Message<A> a = Message.close();
        Message<B> b = Message.close();
        if (aChannel.id() < bChannel.id()) {
            a = aChannel.receive();
            if (a.isValue()) b = bChannel.receive();
        } else {
            b = bChannel.receive();
            if (b.isValue()) a = aChannel.receive();
        }
        var result = Utils.joinMessages(a, b);
        if (!result.isValue()) {
            // automatically cancel both channels
            aChannel.cancel();
            bChannel.cancel();
        }
        return result;
    }

    @Override
    public void cancel() {
        aChannel.cancel();
        bChannel.cancel();
    }
}
