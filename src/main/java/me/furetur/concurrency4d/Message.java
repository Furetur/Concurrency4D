package me.furetur.concurrency4d;

import java.util.Objects;
import java.util.function.Function;

public class Message<T> {
    private final T data;
    
    private static final Message<?> CLOSE = new Message<>(null);
    
    private Message(T data) {
        this.data = data;
    }
    
    static <T> Message<T> value(T t) {
        return new Message<>(Objects.requireNonNull(t));
    }
    
    static <T> Message<T> close() {
        return (Message<T>) CLOSE;
    }


    public boolean isValue() {
        return data != null;
    }
    
    public T value() {
        if (isValue()) {
            return data;
        } else {
            throw new NullPointerException();
        }
    }

    public <R> Message<R> map(Function<T, R> f) {
        if (isValue()) {
            return Message.value(f.apply(value()));
        } else {
            return Message.close();
        }
    }

    @Override
    public String toString() {
        if (isValue()) {
            return data.toString();
        } else {
            return "<CLOSE>";
        }
    }
}
