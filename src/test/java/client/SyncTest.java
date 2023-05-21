package client;

import client.coroutines.CoCollector;
import client.coroutines.CoRange;
import me.furetur.concurrency4d.ConstraintViolatedException;
import me.furetur.concurrency4d.Graph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SyncTest extends CommonTests<Graph> {
    @Override
    protected Graph createGraph() {
        return Graph.create();
    }


    // Constraints checks

    @Test
    void singleSender() {
        var channel = graph.<Long>channel();

        graph.coroutine(new CoRange(channel, 10));
        graph.coroutine(new CoRange(channel, 20));

        assertThrows(ConstraintViolatedException.class, graph::build);
    }

    @Test
    void singleReceiver() {
        var range = graph.<Long>channel();
        graph.coroutine(new CoRange(range, 10));

        var res1 = graph.<List<Long>>channel();
        graph.coroutine(new CoCollector<>(range, res1));


        var res2 = graph.<List<Long>>channel();
        graph.coroutine(new CoCollector<>(range, res2));

        assertThrows(ConstraintViolatedException.class, graph::build);
    }

//    @Test
//    void sendIntoClosedFullChannelDoesNotBlock() {
//        var chan = graph.channel();
//        graph.build();
//
//        chan.send(1);
//        // not the channel is full
//        // however, this close should not block
//        chan.close();
//    }
//
//    @Test
//    void doesNotBlockOnFullButClosedChannel() {
//        var chan = graph.channel();
//        graph.build();
//
//        chan.send(1);
//        chan.close();
//        var wasOpen = chan.send(2); // should not block
//        assertFalse(wasOpen);
//    }
}
