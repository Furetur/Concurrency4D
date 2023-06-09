package client.coroutines;

import com.github.furetur.concurrency4d.AsyncCoroutine;
import com.github.furetur.concurrency4d.SendChannel;

import java.util.List;

public class CoAsyncInterval extends AsyncCoroutine {
    
    private final long interval;
    private final SendChannel<Boolean> channel;
    
    public CoAsyncInterval(SendChannel<Boolean> channel, long interval) {
        super(List.of(), List.of(channel));
        this.interval = interval;
        this.channel = channel;
    }

    @Override
    protected void run() {
        var canSend = true;
        while (canSend) {
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            canSend = channel.send(true);
        }
    }
}
