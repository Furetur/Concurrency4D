package client;


import me.furetur.concurrency4d.Coroutine;
import me.furetur.concurrency4d.Graph;
import me.furetur.concurrency4d.ReceiveChannel;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public final class CoroutinesEagerKMeans {

    private final int dimension;

    private final int forkThreshold;

    //

    public CoroutinesEagerKMeans(final int dimension) {
        this.dimension = dimension;
        // Try to (roughly) fit fork data into half the L2 cache.
        this.forkThreshold = forkThreshold(dimension, (256 / 2) * 1024);
    }

    private int forkThreshold(final int dimension, final int sizeLimit) {
        final int doubleSize = 8 + Double.BYTES;
        final int pointerSize = Long.BYTES;
        final int arraySize = 8 + Integer.BYTES + dimension * pointerSize;
        final int elementSize = arraySize + dimension * doubleSize;
        return sizeLimit / (elementSize + pointerSize);
    }

    public List<Double[]> run(
            final int clusterCount, final List <Double[]> data, final int iterationCount
    ) throws InterruptedException, ExecutionException {
        List<Double[]> centroids = randomSample(clusterCount, data, new Random(100));
        for (int iteration = 0; iteration < iterationCount; iteration++) {
            var graph = Graph.create();
            var assignmentChan = new AssignmentTaskBuilder(graph, data, centroids).build();
            graph.build();
            var assignmentResult = assignmentChan.receive().value();

            graph = Graph.create();
            var updateChan = new UpdateTaskBuilder(graph, assignmentResult).build();
            graph.build();
            var clusters = updateChan.receive().value();

            centroids = new ArrayList<>(clusters.keySet());
        }

        return centroids;
    }


    private List<Double[]> randomSample(
            final int sampleCount, final List<Double[]> data, final Random random
    ) {
        return random.ints(sampleCount, 0, data.size())
                .mapToObj(data::get).collect(Collectors.toList());
    }

    public void tearDown() {
    }


    private static <T> Map<T, List<T>> merge(
            final Map<T, List<T>> left, final Map<T, List<T>> right
    ) {
        //
        // When merging values with the same key, create a new ArrayList to avoid
        // modifying an existing list representing a value in another HashMap.
        //
        final Map<T, List<T>> result = new HashMap<>(left);

        right.forEach((key, val) -> result.merge(
                key, val, (l, r) -> {
                    final List<T> m = new ArrayList<>(l);
                    m.addAll(r);
                    return m;
                }
        ));

        return result;
    }


    abstract class RangedTaskGraphBuilder<T> {
        Graph graph;
        int fromInclusive;
        int toExclusive;
        int taskSize;
        int forkThreshold;

        RangedTaskGraphBuilder(
                Graph graph,
                int fromInclusive,
                int toExclusive,
                int forkThreshold
        ) {
            this.graph = graph;
            this.fromInclusive = fromInclusive;
            this.toExclusive = toExclusive;
            this.taskSize = toExclusive - fromInclusive;
            this.forkThreshold = forkThreshold;
        }

        ReceiveChannel<T> build() {
            if (taskSize < forkThreshold) {
                var compResult = graph.<T>channel(1);
                var coro = new Coroutine(List.of(), List.of(compResult)) {
                    @Override
                    protected void run() {
                        var result = compute(fromInclusive, toExclusive);
                        compResult.send(result);
                    }
                };
                graph.coroutine(coro);
                coro.schedule();
                return compResult;
            } else {
                var middle = fromInclusive + taskSize / 2;
                var left = subgraph(fromInclusive, middle).build();
                var right = subgraph(middle, toExclusive).build();
                var aggResult = graph.<T>channel(1);
                var coro = new Coroutine(List.of(left, right), List.of(aggResult)) {
                    @Override
                    protected void run() {
                        var l = left.receive().value();
                        var r = right.receive().value();
                        aggResult.send(aggregate(l, r));
                    }
                };
                graph.coroutine(coro);
                coro.schedule();
                return aggResult;
            }
        }

        abstract RangedTaskGraphBuilder<T> subgraph(int fromInclusive, int toExclusive);
        abstract T compute(int fromInclusive, int toExclusive);
        abstract T aggregate(T x, T y);
    }

    //

    class AssignmentTaskBuilder extends RangedTaskGraphBuilder<Map<Double[], List<Double[]>>> {
        private final List<Double[]> data;

        private final List<Double[]> centroids;

        AssignmentTaskBuilder(
                Graph graph,
                int fromInclusive,
                int toExclusive,
                List<Double[]> data,
                List<Double[]> centroids
        ) {
            super(graph, fromInclusive, toExclusive, CoroutinesEagerKMeans.this.forkThreshold);
            this.data = data;
            this.centroids = centroids;
        }

        AssignmentTaskBuilder(Graph graph, List<Double[]> data, List<Double[]> centroids) {
            this(graph, 0, data.size(), data, centroids);
        }

        @Override
        RangedTaskGraphBuilder<Map<Double[], List<Double[]>>> subgraph(int fromInclusive, int toExclusive) {
            return new AssignmentTaskBuilder(graph, fromInclusive, toExclusive, data, centroids);
        }

        @Override
        Map<Double[], List<Double[]>> compute(int fromInclusive, int toExclusive) {
            return collectClusters(findNearestCentroid());
        }

        @Override
        Map<Double[], List<Double[]>> aggregate(Map<Double[], List<Double[]>> x, Map<Double[], List<Double[]>> y) {
            return merge(x, y);
        }

        private Map<Double[], List<Double[]>> collectClusters(final int[] centroidIndices) {
            final Map<Double[], List<Double[]>> result = new HashMap<>();

            for (int dataIndex = fromInclusive; dataIndex < toExclusive; dataIndex++) {
                final int centroidIndex = centroidIndices[dataIndex - fromInclusive];
                final Double[] centroid = centroids.get(centroidIndex);
                final Double[] element = data.get(dataIndex);
                result.computeIfAbsent(centroid, k -> new ArrayList<>()).add(element);
            }

            return result;
        }


        private int[] findNearestCentroid() {
            final int[] result = new int[taskSize];

            for (int dataIndex = fromInclusive; dataIndex < toExclusive; dataIndex++) {
                final Double[] element = data.get(dataIndex);

                double min = Double.MAX_VALUE;
                for (int centroidIndex = 0; centroidIndex < centroids.size(); centroidIndex++) {
                    final double distance = distance(element, centroids.get(centroidIndex));
                    if (distance < min) {
                        result[dataIndex - fromInclusive] = centroidIndex;
                        min = distance;
                    }
                }
            }

            return result;
        }


        private double distance(final Double[] x, final Double[] y) {
            //
            // Calculates Euclidean distance between the two points. Note that we
            // don't use sqrt(), because sqrt(a) < sqrt(b) <=> a < b.
            //
            double result = 0.0;
            for (int i = 0; i < dimension; i++) {
                final double diff = x[i] - y[i];
                result += diff * diff;
            }

            return result;
        }
    }

    //

    class UpdateTaskBuilder extends RangedTaskGraphBuilder<Map<Double[], List<Double[]>>> {

        private final List<List<Double[]>> clusters;

        UpdateTaskBuilder(
                Graph graph,
                Map<Double[], List<Double[]>> clusters
        ) {
            this(graph, new ArrayList<>(clusters.values()));
        }

        UpdateTaskBuilder(
                Graph graph,
                List<List<Double[]>> clusters
        ) {
            this(graph, 0, clusters.size(), clusters);
        }

        UpdateTaskBuilder(
                Graph graph,
                int fromInclusive,
                int toExclusive,
                List<List<Double[]>> clusters
        ) {
            super(graph, fromInclusive, toExclusive, 2);
            this.clusters = clusters;
        }

        @Override
        RangedTaskGraphBuilder<Map<Double[], List<Double[]>>> subgraph(int fromInclusive, int toExclusive) {
            return new UpdateTaskBuilder(graph, fromInclusive, toExclusive, clusters);
        }

        @Override
        Map<Double[], List<Double[]>> compute(int fromInclusive, int toExclusive) {
            return computeClusterAverages();
        }

        @Override
        Map<Double[], List<Double[]>> aggregate(Map<Double[], List<Double[]>> x, Map<Double[], List<Double[]>> y) {
            return merge(x, y);
        }

        private Map<Double[], List<Double[]>> computeClusterAverages() {
            final Map<Double[], List<Double[]>> result = new HashMap<>();

            for (int clusterIndex = fromInclusive; clusterIndex < toExclusive; clusterIndex++) {
                final List<Double[]> clusterElements = clusters.get(clusterIndex);
                final Double[] clusterAverage = boxed(average(clusterElements));
                result.put(clusterAverage, clusterElements);
            }

            return result;
        }


        private Double[] boxed(final double[] values) {
            return Arrays.stream(values).boxed().toArray(Double[]::new);
        }


        private double[] average(final List<Double[]> elements) {
            var subgraph = Graph.create();
            var sumChan = new VectorSumTaskBuilder(subgraph, elements).build();
            subgraph.build();
            final double[] vectorSums = sumChan.receive().value();
            return div(vectorSums, elements.size());
        }


        private double[] div(double[] values, int divisor) {
            final double[] result = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                result[i] = values[i] / divisor;
            }

            return result;
        }
    }

    //

    class VectorSumTaskBuilder extends RangedTaskGraphBuilder<double[]> {

        private final List<Double[]> data;

        VectorSumTaskBuilder(Graph graph, List<Double[]> data) {
            this(graph, 0, data.size(), data);
        }

        VectorSumTaskBuilder(
                Graph graph,
                int fromInclusive,
                int toExclusive,
                List<Double[]> data
        ) {
            super(graph, fromInclusive, toExclusive, CoroutinesEagerKMeans.this.forkThreshold);
            this.data = data;
        }

        @Override
        RangedTaskGraphBuilder<double[]> subgraph(int fromInclusive, int toExclusive) {
            return new VectorSumTaskBuilder(graph, fromInclusive, toExclusive, data);
        }

        @Override
        double[] compute(int fromInclusive, int toExclusive) {
            return vectorSum();
        }

        @Override
        double[] aggregate(double[] x, double[] y) {
            return add(x, y);
        }

        private double[] vectorSum() {
            final double[] result = new double[dimension];

            for (int i = fromInclusive; i < toExclusive; i++) {
                accumulate(data.get(i), result);
            }

            return result;
        }

        private void accumulate(final Double[] val, final double[] acc) {
            for (int i = 0; i < dimension; i++) {
                acc[i] += val[i];
            }
        }

        private double[] add(final double[] x, final double[] y) {
            final double[] result = new double[dimension];

            for (int i = 0; i < dimension; i++) {
                result[i] = x[i] + y[i];
            }

            return result;
        }
    }
}
