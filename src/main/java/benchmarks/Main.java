package benchmarks;

import java.util.concurrent.ExecutionException;

public class Main {

    private static int VECTOR_LENGTH = 500000;
    private static int DIMENSION = 5;
    private static int CLUSTER_COUNT = 5;
    private static int ITERATION_COUNT = 50;
    private static int LOOP_COUNT = 4;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        var data = JavaKMeans.generateData(VECTOR_LENGTH, DIMENSION, CLUSTER_COUNT);
        var bench = new JavaKMeans(DIMENSION);
        var result = bench.run(CLUSTER_COUNT, data, ITERATION_COUNT);
        System.out.println(result);
    }
}
