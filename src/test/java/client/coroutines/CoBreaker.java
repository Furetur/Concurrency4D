package client.coroutines;

import com.github.furetur.concurrency4d.Coroutine;
import com.github.furetur.concurrency4d.ReceiveChannel;
import com.github.furetur.concurrency4d.SendChannel;

import java.util.List;

public class CoBreaker<T> extends Coroutine {
    ReceiveChannel<T> in;
    SendChannel<T> out;
    T breaker;

    public CoBreaker(T breaker, ReceiveChannel<T> in, SendChannel<T> out) {
        super(List.of(in), List.of(out));
        this.in = in;
        this.out = out;
        this.breaker = breaker;
    }

    @Override
    protected void run() {
        var canSend = true;
        var msg = in.receive();
        while (msg.isValue() && canSend) {
            if (msg.value() == breaker) break;

            canSend = out.send(msg.value());
            if (canSend) msg = in.receive();
        }
        in.cancel();
        out.close();
    }
}
