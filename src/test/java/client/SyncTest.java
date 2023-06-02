package client;

import client.coroutines.CoCollector;
import client.coroutines.CoRange;
import com.github.furetur.concurrency4d.ConstraintViolatedException;
import com.github.furetur.concurrency4d.Graph;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;


@Disabled
public class SyncTest extends CommonTests<Graph> {

    SyncTest(int channelSize) {
        super(channelSize);
    }

    @Override
    protected Graph createGraph() {
        return Graph.create();
    }


    // Constraints checks

    @Test
    void singleSender() {
        var channel = graph.<Long>channel(CHAN_SIZE);

        graph.coroutine(new CoRange(channel, 10));
        graph.coroutine(new CoRange(channel, 20));

        assertThrows(ConstraintViolatedException.class, graph::build);
    }

    @Test
    void singleReceiver() {
        var range = graph.<Long>channel(CHAN_SIZE);
        graph.coroutine(new CoRange(range, 10));

        var res1 = graph.<List<Long>>channel(CHAN_SIZE);
        graph.coroutine(new CoCollector<>(range, res1));

        var res2 = graph.<List<Long>>channel(CHAN_SIZE);
        graph.coroutine(new CoCollector<>(range, res2));

        assertThrows(ConstraintViolatedException.class, graph::build);
    }
}
