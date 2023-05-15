package client.coroutines;

import com.github.furetur.concurrency4d.Coroutine;
import com.github.furetur.concurrency4d.Log;
import com.github.furetur.concurrency4d.SendChannel;

import java.util.List;

public class CoRange extends Coroutine {
    private final SendChannel<Long> channel;
    private final long from;
    private final long to;

    private final Log log = new Log(this);

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
            long finalI = i;
            log.debug(() -> "sending " + finalI + " into " + channel);
            channel.send(i);
        }
        log.debug(() -> "closing " + channel);
        channel.close();
    }
}
