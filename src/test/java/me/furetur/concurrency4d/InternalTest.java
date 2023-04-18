package me.furetur.concurrency4d;

import client.coroutines.CoAsyncInterval;
import client.coroutines.CoCollector;
import client.coroutines.CoRange;
import me.furetur.concurrency4d.data.Either;
import me.furetur.concurrency4d.data.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InternalTest {
    @Test
    void asyncCoroutineCanSendIntoSyncChannel() {
        var graph = AsyncGraph.create();
        var channel = new SyncChannelImpl<Boolean>(1);
        graph.coroutine(new CoAsyncInterval(channel, 10L));

        graph.build();

        assertTrue(channel.receive().value());
        assertTrue(channel.receive().value());
    }

    @Test
    void mixedRangeAndTimer() {
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
    void unreceiveJoinPutsMessagesInBothChannels() {
        var graph = AsyncGraph.create();

        var chan1 = graph.<Long>channel();
        graph.coroutine(new CoRange(chan1, 0));
        var chan2 = graph.<Long>channel();
        graph.coroutine(new CoRange(chan2, 0));

        var chan = (AsyncJoin<Long, Long>) graph.join(chan1, chan2);
        graph.build();

        chan.unreceive(Message.value(new Pair<>(1L, 2L)));
        assertEquals(1, chan1.receive().value());
        assertEquals(2, chan2.receive().value());
    }

    @Test
    void unreceiveSelectLeftPutsMessageInLeftChannel() {
        var graph = AsyncGraph.create();

        var chan1 = graph.<Long>channel();
        graph.coroutine(new CoRange(chan1, 0));
        var chan2 = graph.<Long>channel();
        graph.coroutine(new CoRange(chan2, 0));

        var chan = (AsyncSelect<Long, Long>) graph.select(chan1, chan2);
        graph.build();

        chan.unreceive(Message.value(new Either.Left<>(1L)));
        assertEquals(1, chan1.receive().value());
        assertFalse(chan2.receive().isValue());
    }

    @Test
    void unreceiveSelectRightPutsMessageInRightChannel() {
        var graph = AsyncGraph.create();

        var chan1 = graph.<Long>channel();
        graph.coroutine(new CoRange(chan1, 0));
        var chan2 = graph.<Long>channel();
        graph.coroutine(new CoRange(chan2, 0));

        var chan = (AsyncSelect<Long, Long>) graph.select(chan1, chan2);
        graph.build();

        chan.unreceive(Message.value(new Either.Right<>(1L)));
        assertFalse(chan1.receive().isValue());
        assertEquals(1, chan2.receive().value());
    }

    // Internal assertions

    @Test
    void doubleCloseBridgePanics() {
        var chan = new AsyncChannelImpl<>();
        chan.close();

    }
}
