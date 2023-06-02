package client;

import client.coroutines.*;
import com.github.furetur.concurrency4d.AsyncGraph;
import com.github.furetur.concurrency4d.data.Either;
import com.github.furetur.concurrency4d.data.Pair;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 10)
public class AsyncTest extends CommonTests<AsyncGraph> {

    AsyncTest() {
        super(1);
    }

    @Override
    protected AsyncGraph createGraph() {
        return AsyncGraph.create();
    }

    @Test
    void selectDoesNotPromoteLeftClose() {
        var chan1 = graph.channel();
        var chan2 = graph.channel();
        var select = graph.select(chan1, chan2);

        graph.build();

        chan1.close();
        chan2.send(1);

        var x = select.receive().value();
        if (x instanceof Either.Right<Object, Object> value) {
            assertEquals(1, (Integer) value.value());
        } else {
            fail();
        }
    }

    @Test
    void selectDoesNotPromoteRightClose() {
        var chan1 = graph.channel();
        var chan2 = graph.channel();
        var select = graph.select(chan1, chan2);

        graph.build();

        chan1.send(1);
        chan2.close();

        var x = select.receive().value();
        if (x instanceof Either.Left<Object, Object> value) {
            assertEquals(1, (Integer) value.value());
        } else {
            fail();
        }
    }

    @Test
    void selectDoesNotPromoteRightCloseWhenLeftIsEmpty() {
        var chan1 = graph.<Boolean>channel();
        var chan2 = graph.channel();
        graph.coroutine(new CoAsyncInterval(chan1, 10L));
        var select = graph.select(chan1, chan2);

        graph.build();

        chan2.close();

        var x = select.receive();
        assertTrue(x.isValue());
    }

    @Test
    void selectOfCloseCloseIsClose() {
        var chan1 = graph.channel();
        var chan2 = graph.channel();
        var select = graph.select(chan1, chan2);

        graph.build();

        chan1.close();
        chan2.close();

        assertFalse(select.receive().isValue());
    }

    @Test
    void selectFlattensTwoRangesOfEqualLength() {
        var size = 100;

        var range1 = graph.<Long>channel();
        graph.coroutine(new CoRange(range1, size));

        var range2 = graph.<Long>channel();
        graph.coroutine(new CoRange(range2, size));

        var chan = graph.select(range1, range2);
        var res = graph.<List<Either<Long, Long>>>channel();
        graph.coroutine(new CoCollector<>(chan, res));

        graph.build();

        List<Long> list = res.receive().value().stream().map(either -> {
            if (either instanceof Either.Left<Long, Long> l) {
                return l.value();
            } else if (either instanceof Either.Right<Long, Long> r) {
                return r.value();
            } else {
                assert false;
                return 0L;
            }
        }).sorted().toList();
        var expected = LongStream.range(0, size).flatMap(i -> LongStream.of(i, i)).boxed().toList();
        assertEquals(expected, list);
    }

    @Test
    void selectFlattensTwoRangesOfUnequalLength() {
        var range1 = graph.<Long>channel();
        graph.coroutine(new CoRange(range1, 5));

        var range2 = graph.<Long>channel();
        graph.coroutine(new CoRange(range2, 10));

        var chan = graph.select(range1, range2);
        var res = graph.<List<Either<Long, Long>>>channel();
        graph.coroutine(new CoCollector<>(chan, res));

        graph.build();

        List<Long> list = res.receive().value().stream().map(either -> {
            if (either instanceof Either.Left<Long, Long> l) {
                return l.value();
            } else if (either instanceof Either.Right<Long, Long> r) {
                return r.value();
            } else {
                assert false;
                return 0L;
            }
        }).sorted().toList();
        var expected = List.of(0L, 0L, 1L, 1L, 2L, 2L, 3L, 3L, 4L, 4L, 5L, 6L, 7L, 8L, 9L);
        assertEquals(expected, list);
    }

    @Test
    void cancelSelect() {
        var chan1 = graph.<Boolean>channel();
        var count1 = graph.<Long>channel();
        graph.coroutine(new CoCancellable(chan1, count1));

        var chan2 = graph.<Boolean>channel();
        var count2 = graph.<Long>channel();
        graph.coroutine(new CoCancellable(chan2, count2));

        var chan = graph.select(chan1, chan2);
        var count = graph.join(count1, count2);

        graph.build();

        // cancel immediately coroutines
        chan.cancel();
        assertEquals(new Pair<>(0L, 0L), count.receive().value());
    }

    @Test
    void selectRollback1() {
        var chan1 = graph.<Long>channel();
        var chan2 = graph.<Long>channel();
        var chan = graph.select(chan1, chan2);

        var range = graph.<Long>channel();
        graph.coroutine(new CoRange(range, 1L));

        var res = graph.join(chan, range);

        graph.build();

        chan1.send(1L);

        assertEquals(new Pair<>(new Either.Left<>(1L), 0L), res.receive().value());
    }

    @Test
    void selectRollback2() {
        var chan1 = graph.<Long>channel();
        var chan2 = graph.<Long>channel();
        var chan = graph.select(chan1, chan2);

        var range = graph.<Long>channel();
        graph.coroutine(new CoRange(range, 1L));

        var res = graph.join(chan, range);

        graph.build();

        chan2.send(1L);

        assertEquals(new Pair<>(new Either.Right<>(1L), 0L), res.receive().value());
    }

    @Test
    void rangeAndTimerBothAsync() {
        var range = graph.<Long>channel();
        graph.coroutine(new CoRange(range, 10));

        var timer = graph.<Boolean>channel();
        graph.coroutine(new CoAsyncInterval(timer, 10L));

        var join = graph.join(range, timer);
        var res = graph.<List<Pair<Long, Boolean>>>channel();
        graph.coroutine(new CoCollector<>(join, res));

        graph.build();

        var list = res.receive().value();
        var expected = LongStream.range(0, 10).mapToObj(i -> new Pair<>(i, true)).toList();
        assertEquals(expected, list);
    }

    @Test
    void timerAndRangeBothAsync() {
        var range = graph.<Long>channel();
        graph.coroutine(new CoRange(range, 10));

        var timer = graph.<Boolean>channel();
        graph.coroutine(new CoAsyncInterval(timer, 10L));

        var join = graph.join(timer, range);
        var res = graph.<List<Pair<Boolean, Long>>>channel();
        graph.coroutine(new CoCollector<>(join, res));

        graph.build();

        var list = res.receive().value();
        var expected = LongStream.range(0, 10).mapToObj(i -> new Pair<>(true, i)).toList();
        assertEquals(expected, list);
    }

    @Test
    void singleCloseDoesNotCloseMultiSenderChannel() {
        var chan = graph.<Long>channel();
        graph.coroutine(new CoRange(chan, 0L)); // will immediately close
        graph.coroutine(new CoRange(chan, 1L)); // will send a single message
        graph.build();

        assertEquals(0L, chan.receive().value());
    }

    @Test
    void singleCancelDoesNotCloseMultiReceiverChannel() {
        var multireceiver = graph.<Boolean>channel();
        var count = graph.<Long>channel();
        graph.coroutine(new CoCancellable(multireceiver, count));

        var map = graph.<Boolean>channel();
        graph.coroutine(new CoMap<>(i -> i, multireceiver, map));
        graph.coroutine(new CoMap<>(i -> i, multireceiver, graph.channel()));

        graph.build();

        // multireceiver is a channel between CoCancellable and 2 CoMaps
        // we cancel the first CoMap, it promotes the cancellation
        // if it closes the multireceiver channel, the final count will be 0
        // otherwise it will be MAX_COUNT
        map.cancel();
        assertEquals(CoCancellable.MAX_COUNT, count.receive().value());
    }

    @Test
    void halfClosedJoinRollback() {
        var empty = graph.channel();
        graph.coroutine(new CoEmpty(empty));

        var closed = graph.<Long>channel();
        graph.coroutine(new CoRange(closed, 0L));

        // this join should be closed immediately once CoRange is scheduled
        var closedJoin = graph.join(empty, closed);

        var singleValue = graph.<Long>channel();
        graph.coroutine(new CoRange(singleValue, 1L));

        var select = graph.select(closedJoin, singleValue);
        graph.build();

        // by receiving from select, we first tryReceive from closed join and roll it back
        // this test checks that this rollback works correctly
        assertEquals(new Either.Right<>(0L), select.receive().value());
    }

    @Test
    void halfClosedJoinRollback2() {
        // This is a pair test for the test defined directly above
        // The only difference is the order of parameters to select
        var empty = graph.channel();
        graph.coroutine(new CoEmpty(empty));

        var closed = graph.<Long>channel();
        graph.coroutine(new CoRange(closed, 0L));

        var closedJoin = graph.join(empty, closed);

        var singleValue = graph.<Long>channel();
        graph.coroutine(new CoRange(singleValue, 1L));

        var select = graph.select(singleValue, closedJoin);
        graph.build();

        assertEquals(new Either.Left<>(0L), select.receive().value());
    }

    @Test
    void closedSelectRollback() {
        var closed = graph.<Long>channel();
        graph.coroutine(new CoRange(closed, 0L));

        var closedSelect = graph.select(closed, closed);

        var singleValue = graph.<Long>channel();
        graph.coroutine(new CoRange(singleValue, 1L));

        var select = graph.select(closedSelect, singleValue);
        graph.build();

        // the select will first receive from closedSelect
        // when it notices that the result is CLOSE it will roll back
        // this test checks that the rollback works
        assertEquals(new Either.Right<>(0L), select.receive().value());
    }

    @Test
    void closedSelectRollback2() {
        // see the test directly above
        var closed = graph.<Long>channel();
        graph.coroutine(new CoRange(closed, 0L));

        var closedSelect = graph.select(closed, closed);

        var singleValue = graph.<Long>channel();
        graph.coroutine(new CoRange(singleValue, 1L));

        var select = graph.select(singleValue, closedSelect);
        graph.build();

        assertEquals(new Either.Left<>(0L), select.receive().value());
    }

    @RepeatedTest(1000)
    void joinRangesViceVersa() {
        var range1 = graph.<Long>channel();
        graph.coroutine(new CoRange(range1, 1));

        var range2 = graph.<Long>channel();
        graph.coroutine(new CoRange(range2, 0));

        var mapped = graph.<Long>channel();
        graph.coroutine(new CoMap<>(i -> i * i, range2, mapped));

        var res = graph.join(mapped, range1);

        graph.build();

        assertFalse(res.receive().isValue());
    }
}
