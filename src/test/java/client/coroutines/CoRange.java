package client.coroutines;

import me.furetur.concurrency4d.Coroutine;
import me.furetur.concurrency4d.SendChannel;

import java.util.List;

public class CoRange extends Coroutine {
    private final SendChannel<Long> channel;
    private final long from;
    private final long to;

    public CoRange(SendChannel<Long> out, long from, long to) {
        super(List.of(), List.of(out));
        this.channel = out;
        this.from = from;
        this.to = to;
    }

    public CoRange(SendChannel<Long> out, long to) {
        this(out, 0, to);
    }

    @Override
    protected void run() {
        for (long i = from; i < to; i++) {
            channel.send(i);
        }
        channel.close();
    }
}
