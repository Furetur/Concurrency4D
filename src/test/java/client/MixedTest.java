package client;

import client.coroutines.CoCollector;
import client.coroutines.CoRange;
import me.furetur.concurrency4d.AsyncGraph;
import me.furetur.concurrency4d.Graph;
import me.furetur.concurrency4d.data.Either;
import me.furetur.concurrency4d.data.Pair;
import client.coroutines.CoAsyncInterval;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MixedTest {

    // ===== Join =====

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

    @Test
    void rangeAndTimerViceVersa() {
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
        var join = agraph.join(timer, range);
        var res = agraph.<List<Pair<Boolean, Long>>>channel();
        agraph.coroutine(new CoCollector<>(join, res));
        agraph.build();

        var list = res.receive().value();
        var expected = List.of(new Pair<>(true, 0L), new Pair<>(true, 1L));
        assertEquals(expected, list);
    }

    @Test
    void asyncJoinOfSyncChannels() {
        // sync range
        var graph = Graph.create();
        var range1 = graph.<Long>channel();
        graph.coroutine(new CoRange(range1, 2));
        var range2 = graph.<Long>channel();
        graph.coroutine(new CoRange(range2, 10, 12));
        graph.build();

        // async join
        var agraph = AsyncGraph.create();
        var join = agraph.join(range1, range2);
        var res = agraph.<List<Pair<Long, Long>>>channel();
        agraph.coroutine(new CoCollector<>(join, res));
        agraph.build();

        var list = res.receive().value();
        var expected = List.of(new Pair<>(0L, 10L), new Pair<>(1L, 11L));
        assertEquals(expected, list);
    }

    // ===== Select =====

    @Test
    void selectOfSyncChannels() {
        // sync range
        var graph = Graph.create();
        var range1 = graph.<Long>channel();
        graph.coroutine(new CoRange(range1, 2));
        var range2 = graph.<Long>channel();
        graph.coroutine(new CoRange(range2, 10, 12));
        graph.build();

        // async join
        var agraph = AsyncGraph.create();
        var join = agraph.select(range1, range2);
        var res = agraph.<List<Either<Long, Long>>>channel();
        agraph.coroutine(new CoCollector<>(join, res));
        agraph.build();

        var list = res.receive().value().stream().map(either ->
                either instanceof Either.Left<Long, Long> ?
                        ((Either.Left<Long, Long>) either).value() : ((Either.Right<Long, Long>) either).value()
        ).sorted().toList();
        var expected = List.of(0L, 1L, 10L, 11L);
        assertEquals(expected, list);
    }
}
