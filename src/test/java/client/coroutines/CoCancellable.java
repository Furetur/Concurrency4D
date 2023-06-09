package client.coroutines;

import com.github.furetur.concurrency4d.Coroutine;
import com.github.furetur.concurrency4d.SendChannel;
import com.github.furetur.concurrency4d.Log;


import java.util.List;

public class CoCancellable extends Coroutine {

    public static final long MAX_COUNT = 100;

    private final SendChannel<Boolean> out;
    private final SendChannel<Long> quit;

    private final Log log = new Log(this);

    public CoCancellable(SendChannel<Boolean> out, SendChannel<Long> quit) {
        super(List.of(), List.of(out, quit));
        this.out = out;
        this.quit = quit;
    }

    @Override
    protected void run() {
        var count = 0L;
        log.debug("sending a value");
        var wasSent = out.send(true);
        while (wasSent && count < MAX_COUNT) {
            count++;
            log.debug("sending a value");
            wasSent = out.send(true);
        }
        log.debug("the channel was cancelled, managed to send " + count + " times");
        quit.send(count);
    }
}
