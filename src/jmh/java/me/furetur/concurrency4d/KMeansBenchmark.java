package me.furetur.concurrency4d;

import org.openjdk.jmh.annotations.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

@BenchmarkMode(Mode.SampleTime)
@Fork(1)
@State(Scope.Benchmark)
public class KMeansBenchmark {

    private int VECTOR_LENGTH = 50000;
    private int DIMENSION = 5;
    private int CLUSTER_COUNT = 5;
    private int ITERATION_COUNT = 50;

    private List<Double[]> data;

    @Setup
    public void generateData() {
        data = JavaKMeans.generateData(VECTOR_LENGTH, DIMENSION, CLUSTER_COUNT);
    }

    @Warmup(iterations = 20)
    @Measurement(iterations = 10)
    @Benchmark
    public List<Double[]> javaForkJoin() throws ExecutionException, InterruptedException {
        var benchmark = new JavaKMeans(DIMENSION);
        var res = benchmark.run(CLUSTER_COUNT, data, ITERATION_COUNT);
        benchmark.tearDown();
        return res;
    }

    @Warmup(iterations = 20)
    @Measurement(iterations = 10)
    @Benchmark
    public List<Double[]> concurrency4d() throws ExecutionException, InterruptedException {
        var bench = new CoroutinesKMeans(DIMENSION);
        return bench.run(CLUSTER_COUNT, data, ITERATION_COUNT);
    }
}
