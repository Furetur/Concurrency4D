package client.coroutines;

import com.github.furetur.concurrency4d.Coroutine;
import com.github.furetur.concurrency4d.SendChannel;

import java.util.List;

public class CoEmpty extends Coroutine {
    public CoEmpty(SendChannel<?> channel) {
        super(List.of(), List.of(channel));
    }

    @Override
    protected void run() {
    }
}
