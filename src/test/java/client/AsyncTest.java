package client;

import me.furetur.concurrency4d.AsyncGraph;
import me.furetur.concurrency4d.Graph;

public class AsyncTest extends CommonTests {
    @Override
    protected Graph createGraph() {
        return AsyncGraph.create();
    }
}
