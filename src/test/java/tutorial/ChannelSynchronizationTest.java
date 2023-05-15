package tutorial;

import com.github.furetur.concurrency4d.AsyncCoroutine;
import com.github.furetur.concurrency4d.AsyncGraph;
import com.github.furetur.concurrency4d.SendChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;

// https://gobyexample.com/channel-synchronization
public class ChannelSynchronizationTest {
    static class Worker extends AsyncCoroutine {
        SendChannel<Boolean> done;
        Worker(SendChannel<Boolean> done) {
            super(List.of(), List.of(done));
            this.done = done;
        }
        @Override
        protected void run() {
            System.out.println("working...");
            sleep(1000);
            System.out.println("done");

            done.send(true);
        }

        void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    @Timeout(3000)
    void main() {
        var graph = AsyncGraph.create();

        var done = graph.<Boolean>channel();
        graph.coroutine(new Worker(done));

        graph.build();

        done.receive();
    }
}
