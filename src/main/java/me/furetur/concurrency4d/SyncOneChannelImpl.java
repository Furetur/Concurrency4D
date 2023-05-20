package me.furetur.concurrency4d;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class SyncOneChannelImpl<T> implements InternalChannel<T> {
    private final long _id = Utils.nextId();
    private T data = null;
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
        try {
            lock.lock();

            while (data == null) {
                schedule(sender);
                notEmptyOrClosed.await();
            }

            var value = data;
            data = null;
            notFullOrClosed.signal();
            return Message.value(value);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void cancel() {
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
        try {
            lock.lock();

            while (data != null) {
                schedule(receiver);
                notFullOrClosed.await();
            }

            data = value;
            schedule(receiver);
            notEmptyOrClosed.signal();
            return true;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean close() {
        return true;
    }

    private static void schedule(AsyncCoroutine coroutine) {
        if (coroutine != null) {
            coroutine.schedule();
        }
    }

    //

    private final State<T> emptyState = (State<T>) State.EMPTY;
    private final State<T> closedState = (State<T>) State.CLOSED;

    static <T> State<T> valueState(T value) {
        return new State<>(value, false);
    }

    record State<T>(T value, boolean closed) {
        private static State<?> EMPTY = new State<>(null, false);
        private static State<?> CLOSED = new State<>(null, true);
    }
}
