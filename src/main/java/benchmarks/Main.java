package benchmarks;

import me.furetur.concurrency4d.Log;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class Main {

    private static int VECTOR_LENGTH = 50000;
    private static int DIMENSION = 5;
    private static int CLUSTER_COUNT = 5;
    private static int ITERATION_COUNT = 50;
    private static int LOOP_COUNT = 4;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        var data = generateData();

        var N = 10;
        var javaTotalTime = 0L;
        var coroTotalTime = 0L;
        for (int i = 0; i < N; i++) {
            long time;
            time = System.nanoTime();
            var r1 = java(data);
            time = System.nanoTime() - time;
            javaTotalTime += time;

            time = System.nanoTime();
            var r2 = coroutines(data);
            time = System.nanoTime() - time;
            coroTotalTime += time;

            compareResults(r1, r2);
            Log.flush();
        }
        System.out.println("Java Time:" + javaTotalTime);
        System.out.println("Coro Time:" + coroTotalTime);
        System.out.println("+" + ((double)coroTotalTime) / javaTotalTime * 100 + "%");
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

    public static void compareResults(List<Double[]> java, List<Double[]> coro) {
        System.out.println("Java");
        printList(java);
        System.out.println("\nCoro");
        printList(coro);
        if (!isEqual(java, coro)) {
            throw new RuntimeException("results are not equal");
        }
    }

    public static boolean isEqual(List<Double[]> list1, List<Double[]> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }

        list1.sort(Arrays::compare);
        list2.sort(Arrays::compare);

        double epsilon = 0.000001d;
        for (int i = 0; i < list1.size(); i++) {
            Double[] array1 = list1.get(i);
            Double[] array2 = list2.get(i);

            if (array1.length != array2.length) {
                return false;
            }

            for (int j = 0; j < array1.length; j++) {
                if (Math.abs(array1[j] - array2[j]) > epsilon) {
                    return false;
                }
            }
        }

        return true;
    }


    public static void printList(List<Double[]> x) {
        for (int i = 0; i < x.size(); i++) {
            var arr = x.get(i);
            System.out.println(i + ": " + List.of(arr));
        }
    }

    public static List<Double[]> generateData() {
        return JavaKMeans.generateData(VECTOR_LENGTH, DIMENSION, CLUSTER_COUNT);
    }
}
