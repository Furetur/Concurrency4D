package client;

import client.coroutines.CoAsyncInterval;
import client.coroutines.CoCollector;
import client.coroutines.CoRange;
import me.furetur.concurrency4d.AsyncGraph;
import me.furetur.concurrency4d.Graph;
import me.furetur.concurrency4d.data.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MixedTest {
    @Test
    void rangeAndTimer() {
        // sync range
        var graph = Graph.create();
        var range = graph.<Long>channel();
        graph.coroutine(new CoRange(range, 2));
        graph.build();

        // async timer
        var agraph = AsyncGraph.create();
        var timer = agraph.<Boolean>channel();
        agraph.coroutine(new CoAsyncInterval(timer, 10L));

        // join
        var join = agraph.join(range, timer);
        var res = agraph.<List<Pair<Long, Boolean>>>channel();
        agraph.coroutine(new CoCollector<>(join, res));
        agraph.build();

        var list = res.receive().value();
        var expected = List.of(new Pair<>(0L, true), new Pair<>(1L, true));
        assertEquals(expected, list);
    }
}
