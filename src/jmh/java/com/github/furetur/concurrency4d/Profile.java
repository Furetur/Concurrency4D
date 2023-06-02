package com.github.furetur.concurrency4d;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class Profile {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        var data = VerifyResults.generateData();

        for (int i = 0; i < 10; i++) {
            List<Double[]> r;
            r = VerifyResults.coroutines(data);
            System.out.println(r);
            r = VerifyResults.java(data);
            System.out.println(r);
        }
        Log.flush();
    }
}
