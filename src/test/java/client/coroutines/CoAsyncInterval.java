package client.coroutines;

import me.furetur.concurrency4d.AsyncCoroutine;
import me.furetur.concurrency4d.ReceiveChannel;
import me.furetur.concurrency4d.SendChannel;

import java.util.List;

public class CoAsyncInterval extends AsyncCoroutine {
    
    private long interval;
    private SendChannel<Boolean> channel;
    
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
