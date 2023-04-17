package client;

import client.coroutines.CoCancellable;
import client.coroutines.CoCollector;
import client.coroutines.CoMap;
import client.coroutines.CoRange;
import me.furetur.concurrency4d.Graph;
import me.furetur.concurrency4d.data.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public abstract class CommonTests {

    protected Graph graph;

    abstract protected Graph createGraph();

    @BeforeEach
    void setUp() {
        graph = createGraph();
    }

    @Test
    void range() {
        var range = graph.<Long>channel();
        graph.coroutine(new CoRange(range, 10));

        var quit = graph.<List<Long>>channel();
        graph.coroutine(new CoCollector<>(range, quit));

        graph.build();

        var list = quit.receive().value();

        assertEquals(
                LongStream.range(0, 10).boxed().collect(Collectors.toList()),
                list
        );
    }

    @Test
    void joinRanges() {
        var range1 = graph.<Long>channel();
        graph.coroutine(new CoRange(range1, 10));

        var range2 = graph.<Long>channel();
        graph.coroutine(new CoRange(range2, 10));

        var mapped = graph.<Long>channel();
        graph.coroutine(new CoMap<>(i -> i * i, range2, mapped));

        var res = graph.join(range1, mapped);

        var quit = graph.<List<Pair<Long, Long>>>channel();
        graph.coroutine(new CoCollector<>(res, quit));

        graph.build();

        var list = quit.receive().value();

        assertEquals(
                LongStream.range(0, 10).mapToObj(i -> new Pair<>(i, i * i)).collect(Collectors.toList()),
                list
        );
    }

    @Test
    void simpleJoin() {
        var range1 = graph.<Long>channel();
        graph.coroutine(new CoRange(range1, 10));

        var range2 = graph.<Long>channel();
        graph.coroutine(new CoRange(range2, 10));

        var res = graph.join(range1, range2);

        var quit = graph.<List<Pair<Long, Long>>>channel();
        graph.coroutine(new CoCollector<>(res, quit));

        graph.build();

        var list = quit.receive().value();

        assertEquals(
                LongStream.range(0, 10).mapToObj(i -> new Pair<>(i, i)).collect(Collectors.toList()),
                list
        );
    }

    @Test
    void joinRangesViceVersa() {
        var range1 = graph.<Long>channel();
        graph.coroutine(new CoRange(range1, 10));

        var range2 = graph.<Long>channel();
        graph.coroutine(new CoRange(range2, 10));

        var mapped = graph.<Long>channel();
        graph.coroutine(new CoMap<>(i -> i * i, range2, mapped));

        var res = graph.join(mapped, range1);

        var quit = graph.<List<Pair<Long, Long>>>channel();
        graph.coroutine(new CoCollector<>(res, quit));

        graph.build();

        var list = quit.receive().value();

        assertEquals(
                LongStream.range(0, 10).mapToObj(i -> new Pair<>(i * i, i)).collect(Collectors.toList()),
                list
        );
    }

    @Test
    void joinShouldPromoteClose() {
        var range = graph.<Long>channel();
        graph.coroutine(new CoRange(range, 10));

        var close = graph.channel(1);
        var join = graph.join(range, close);

        graph.build();

        close.close();
        assertFalse(join.receive().isValue());
    }

    @Test
    void joiningClosesShouldResultInClose() {
        var chan1 = graph.channel(1);
        var chan2 = graph.channel(1);

        var join = graph.join(chan1, chan2);

        graph.build();

        chan1.close();
        chan2.close();
        assertFalse(join.receive().isValue());
    }

    @Test
    void shouldCancel() {
        var cancel = graph.<Boolean>channel();
        var count = graph.<Long>channel();
        graph.coroutine(new CoCancellable(cancel, count));

        graph.build();
        cancel.cancel();

        assertEquals(0, count.receive().value());
    }

    @Test
    void shouldCancelBoth() {
        var cancel1 = graph.<Boolean>channel();
        var count1 = graph.<Long>channel();
        graph.coroutine(new CoCancellable(cancel1, count1));

        var cancel2 = graph.<Boolean>channel();
        var count2 = graph.<Long>channel();
        graph.coroutine(new CoCancellable(cancel2, count2));

        var cancel = graph.join(cancel1, cancel2);
        var count = graph.join(count1, count2);

        graph.build();

        cancel.cancel();
        assertEquals(new Pair<>(0L, 0L), count.receive().value());
    }

    @Test
    void secondOrderJoin() {
        var range1 = graph.<Long>channel();
        graph.coroutine(new CoRange(range1, 10));

        var range2 = graph.<Long>channel();
        graph.coroutine(new CoRange(range2, 10));

        var range3 = graph.<Long>channel();
        graph.coroutine(new CoRange(range3, 10));

        var join = graph.join(graph.join(range1, range2), range3);
        var res = graph.<List<Pair<Pair<Long, Long>, Long>>>channel();
        graph.coroutine(new CoCollector<>(join, res));

        graph.build();

        var list = res.receive().value();
        assertEquals(
                LongStream.range(0, 10).mapToObj(i -> new Pair<>(new Pair<>(i, i), i)).collect(Collectors.toList()),
                list
        );
    }
}
