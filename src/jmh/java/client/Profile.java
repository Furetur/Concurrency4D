package client;

import me.furetur.concurrency4d.Log;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class Profile {

    private static int VECTOR_LENGTH = 50000;
    private static int DIMENSION = 5;
    private static int CLUSTER_COUNT = 5;
    private static int ITERATION_COUNT = 50;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        var data = generateData();

        for (int i = 0; i < 10; i++) {
            var r = coroutines(data);
//            var r = VerifyResults.loom(data);
            System.out.println(r);
        }
        Log.flush();
    }

    public static List<Double[]> java(List<Double[]> data) throws ExecutionException, InterruptedException {
        var bench = new JavaKMeans(DIMENSION);
        var res = bench.run(CLUSTER_COUNT, data, ITERATION_COUNT);
        bench.tearDown();
        return res;
    }

    public static List<Double[]> coroutines(List<Double[]> data) throws ExecutionException, InterruptedException {
        var bench = new CoroutinesKMeans(DIMENSION);
        return bench.run(CLUSTER_COUNT, data, ITERATION_COUNT);
    }

    public static List<Double[]> generateData() {
        return JavaKMeans.generateData(VECTOR_LENGTH, DIMENSION, CLUSTER_COUNT);
    }
}
