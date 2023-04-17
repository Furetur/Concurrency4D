package client.coroutines;

import me.furetur.concurrency4d.Coroutine;
import me.furetur.concurrency4d.SendChannel;

import java.util.List;

public class CoCancellable extends Coroutine {

    private static long MAX_COUNT = 100;

    private SendChannel<Boolean> out;
    private SendChannel<Long> quit;

    public CoCancellable(SendChannel<Boolean> out, SendChannel<Long> quit) {
        super(List.of(), List.of(out, quit));
        this.out = out;
        this.quit = quit;
    }

    @Override
    protected void run() {
        var count = 0L;
        var wasSent = out.send(true);
        while (wasSent && count < MAX_COUNT) {
            count++;
            wasSent = out.send(true);
        }
        quit.send(count);
    }
}
