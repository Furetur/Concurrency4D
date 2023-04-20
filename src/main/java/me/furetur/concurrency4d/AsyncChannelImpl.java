package me.furetur.concurrency4d;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

class AsyncChannelImpl<T> implements InternalAsyncChannel<T> {

    private final LinkedList<T> data = new LinkedList<>();

    private int cancellations;
    private int closures;

    private final List<AsyncCoroutine> senders = new CopyOnWriteArrayList<>();
    private final List<AsyncCoroutine> receivers = new CopyOnWriteArrayList<>();
    private Thread receiveBridgeThread;

    private final Lock lock = new ReentrantLock();
    private final Condition notEmptyOrClosed = lock.newCondition();

    @Override
    public long id() {
        return 0;
    }

    @Override
    public void registerReceiver(AsyncCoroutine coroutine) {
        receivers.add(coroutine);
    }

    private void setUpBridgeIfNeeded() {
        if (isReceiveBridge() && receiveBridgeThread == null) {
            System.out.println("set up bridge on " + this);
            receiveBridgeThread = Thread.currentThread();
        }
    }

    public Message<T> receive() {
        try {
            lock.lock();

            setUpBridgeIfNeeded();

            while (data.size() == 0 && !isClosed()) {
                scheduleSenders();
                notEmptyOrClosed.await();
            }

            if (data.size() > 0) {
                var x = data.pollFirst();
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
    public Optional<Message<T>> tryReceive() {
        try {
            lock.lock();

            setUpBridgeIfNeeded();

            if (data.size() > 0) {
                var x = data.pollFirst();
                return Optional.of(Message.value(x));
            } else if (isClosed()) {
                return Optional.of(Message.close());
            } else {
                scheduleSenders();
                return Optional.empty();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void unreceive(Message<T> message) {
        try {
            lock.lock();

            if (message.isValue()) {
                data.addFirst(message.value());
            } else {
                assert isClosed();
            }
        } finally {
            lock.unlock();
        }
    }

    public void cancel() {
        try {
            lock.lock();

            if (!isCancelled()) {
                cancellations++;
                // Cancel closes the channel. So if the channel is already closed, there is nothing to do
                if (isCancelled() && !isClosed()) {
                    // close
                    data.clear();
                    setClosed();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void registerSender(AsyncCoroutine coroutine) {
        senders.add(coroutine);
    }

    public boolean send(T value) {
        try {
            lock.lock();

            // if is closed for send
            if (isClosed()) {
                return false;
            } else {
                data.addLast(value);
                scheduleReceivers();
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean close() {
        try {
            lock.lock();

            if (isClosed()) {
                return false;
            } else {
                closures++;
                if (isClosed()) {
                    // close
                    scheduleReceivers();
                }
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    private boolean isCancelled() {
        if (isReceiveBridge()) {
            assert cancellations <= 1;
            return cancellations == 1;
        } else {
            assert cancellations <= receivers.size();
            return cancellations == receivers.size();
        }
    }

    private boolean isClosed() {
        if (isSendBridge()) {
            assert closures <= 1;
            return closures == 1;
        } else {
            assert closures <= senders.size();
            return closures == senders.size();
        }
    }

    private void setClosed() {
        if (isSendBridge()) {
            closures = 1;
        } else {
            closures = senders.size();
        }
    }

    private boolean isReceiveBridge() {
        return receivers.size() == 0;
    }

    private boolean isSendBridge() {
        return senders.size() == 0;
    }

    private void scheduleReceivers() {
        var header = "scheduling receivers of " + this + "\n";
        notEmptyOrClosed.signalAll();
        if (isReceiveBridge()) {
            if (receiveBridgeThread != null) {
                System.out.println(header + "\tunparking bridge");
                LockSupport.unpark(receiveBridgeThread);
            } else {
                System.out.println(header + "\tthis is a receive bridge, but the thread is not set up");
            }
        } else {
            System.out.println(header + "\tscheduling receiver coroutines: " + receivers);
            schedule(receivers);
        }
    }

    private void scheduleSenders() {
        System.out.println("scheduling senders");
        schedule(senders);
    }

    private static void schedule(List<? extends AsyncCoroutine> coroutines) {
        for (var coroutine : coroutines) {
            coroutine.schedule();
        }
    }
}
