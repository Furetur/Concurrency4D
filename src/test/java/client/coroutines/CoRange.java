package client.coroutines;

import me.furetur.concurrency4d.Coroutine;
import me.furetur.concurrency4d.SendChannel;

import java.util.List;

public class CoRange extends Coroutine {
    private final SendChannel<Long> channel;
    private final long n;

    public CoRange(SendChannel<Long> out, long n) {
        super(List.of(), List.of(out));
        this.channel = out;
        this.n = n;
    }

    @Override
    protected void run() {
        for (long i = 0; i < n; i++) {
            channel.send(i);
        }
        channel.close();
    }
}
