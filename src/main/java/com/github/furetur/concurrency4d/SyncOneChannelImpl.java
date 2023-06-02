package com.github.furetur.concurrency4d;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class SyncOneChannelImpl<T> implements InternalChannel<T> {
    private final long _id = Utils.nextId();
    private Message<T> data = null;
    private volatile boolean isClosed = false;
    private AsyncCoroutine sender;
    private AsyncCoroutine receiver;

    private final Lock lock = new ReentrantLock();
    private final Condition notEmptyOrClosed = lock.newCondition();
    private final Condition notFullOrClosed = lock.newCondition();

    @Override
    public long id() {
        return _id;
    }

    @Override
    public void registerReceiver(AsyncCoroutine coroutine) {
        if (receiver != null) {
            throw new ConstraintViolatedException("Sync channel must have not more than 1 receiver");
        }
        receiver = coroutine;
    }

    @Override
    public Message<T> receive() {
        if (isClosed) return Message.close();

        try {
            lock.lock();

            while (data == null) {
                schedule(sender);
                notEmptyOrClosed.await();
            }

            var message = data;
            data = null;
            notFullOrClosed.signal();
            return message;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void cancel() {
        try {
            lock.lock();

            isClosed = true;
            data = null;
            notFullOrClosed.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void registerSender(AsyncCoroutine coroutine) {
        if (sender != null) {
            throw new ConstraintViolatedException("Sync channel must have not more than 1 sender");
        }
        sender = coroutine;
    }

    @Override
    public boolean send(T value) {
        return sendMessage(Message.value(value));
    }

    @Override
    public boolean close() {
        var res = sendMessage(Message.close());
        isClosed = true;
        return res;
    }

    private boolean sendMessage(Message<T> msg) {
        if (isClosed) return false;

        try {
            lock.lock();

            while (data != null) {
                schedule(receiver);
                notFullOrClosed.await();
            }

            data = msg;
            schedule(receiver);
            notEmptyOrClosed.signal();
            return true;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private static void schedule(AsyncCoroutine coroutine) {
        if (coroutine != null) {
            coroutine.schedule();
        }
    }
}
