package client.coroutines;

import me.furetur.concurrency4d.*;

import java.util.List;
import java.util.function.Function;

public class CoMap<T, R> extends Coroutine {

    Function<T, R> f;
    ReceiveChannel<T> in;
    SendChannel<R> out;

    public CoMap(Function<T, R> f, ReceiveChannel<T> in, SendChannel<R> out) {
        super(List.of(in), List.of(out));
        this.f = f;
        this.in = in;
        this.out = out;
    }

    @Override
    protected void run() {
        var canSend = true;
        var msg = in.receive();
        while (msg.isValue() && canSend) {
            var y = f.apply(msg.value());
            canSend = out.send(y);
            if (canSend) msg = in.receive();
        }
        in.cancel();
        out.close();
    }
}
