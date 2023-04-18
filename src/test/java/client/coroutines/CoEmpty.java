package client.coroutines;

import me.furetur.concurrency4d.Coroutine;
import me.furetur.concurrency4d.ReceiveChannel;
import me.furetur.concurrency4d.SendChannel;

import java.util.List;

public class CoEmpty extends Coroutine {
    public CoEmpty(SendChannel<?> channel) {
        super(List.of(), List.of(channel));
    }

    @Override
    protected void run() {
    }
}
