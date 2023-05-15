package com.github.furetur.concurrency4d;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class SyncChannelImpl<T> implements InternalChannel<T> {
    private final long _id;
    private final LinkedList<T> data = new LinkedList<>();
    private boolean isClosed;
    private final int maxSize;

    private AsyncCoroutine sender;
    private AsyncCoroutine receiver;

    private final Lock lock = new ReentrantLock();
    private final Condition notEmptyOrClosed = lock.newCondition();
    private final Condition notFullOrClosed = lock.newCondition();

    SyncChannelImpl(int size) {
        this._id = Utils.nextId();
        this.maxSize = size;
    }

    @Override
    public long id() {
        return this._id;
    }

    @Override
    public void registerSender(AsyncCoroutine coroutine) {
        if (sender != null) {
            throw new ConstraintViolatedException("Sync channel must have not more than 1 sender");
        }
        sender = coroutine;
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
        try {
            lock.lock();

            while (data.size() == 0 && !isClosed) {
                schedule(sender);
                notEmptyOrClosed.await();
            }

            if (data.size() > 0) {
                var x = data.pollFirst();
                notFullOrClosed.signal();
                return Message.value(x);
            } else {
                return Message.close();
            }
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

            data.clear();
            isClosed = true;
            notFullOrClosed.signal();

        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean send(T value) {
        try {
            lock.lock();

            while (data.size() == maxSize && !isClosed) {
                schedule(receiver);
                notFullOrClosed.await();
            }

            if (isClosed) {
                return false;
            } else {
                data.offerLast(value);
                schedule(receiver);
                notEmptyOrClosed.signal();
                return true;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean close() {
        try {
            lock.lock();

            if (isClosed) {
                return false;
            } else {
                isClosed = true;
                schedule(receiver);
                notEmptyOrClosed.signal();
                return true;
            }
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
