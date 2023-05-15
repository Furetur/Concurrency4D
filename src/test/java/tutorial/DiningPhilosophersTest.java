package tutorial;

import me.furetur.concurrency4d.*;
import me.furetur.concurrency4d.data.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DiningPhilosophersTest {

    int N = 5;
    @Test
    @Timeout(6000)
    void diningPhilosophers() {
        // setup
        var graph = AsyncGraph.create();
        var forks = new ArrayList<Channel<Fork>>();
        for (int i = 0; i < N; i++) {
            forks.add(graph.channel());
        }
        var quit = graph.<Boolean>channel();
        for (int i = 0; i < N; i++) {
            var l = forks.get(i % N);
            var r = forks.get((i + 1) % N);
            graph.coroutine(new Philosopher(i, graph.join(l, r), l, r, quit));
        }
        graph.build();

        for (int i = 0; i < N; i++) {
            forks.get(i).send(new Fork(i));
        }

        // start
        for (int i = 0; i < N; i++) {
            quit.receive();
            System.out.println("Philosopher exited");
        }
        System.out.println("Finished");
    }

    static class Philosopher extends AsyncCoroutine {

        int id;
        ReceiveChannel<Pair<Fork, Fork>> forks;
        SendChannel<Fork> leftFork;
        SendChannel<Fork> rightFork;

        SendChannel<Boolean> quit;

        Philosopher(
                int id,
                ReceiveChannel<Pair<Fork, Fork>> forks,
                SendChannel<Fork> leftFork,
                SendChannel<Fork> rightFork,
                SendChannel<Boolean> quit
        ) {
            super(List.of(forks), List.of(leftFork, rightFork, quit));
            this.id = id;
            this.forks = forks;
            this.leftFork = leftFork;
            this.rightFork = rightFork;
            this.quit = quit;
        }

        @Override
        protected void run() {
            for (int i = 0; i < 2; i++) {
                var f = forks.receive();
                eat();
                leftFork.send(f.value().first());
                rightFork.send(f.value().second());
                sleep();
            }
            quit.send(true);
        }

        void eat() {
            var time = randomTime();
            log("eating for " + time);
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        void sleep() {
            var time = randomTime();
            log("sleeping for " + time);
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        long randomTime() {
            return new Random().nextInt(1, 5) * 100L;
        }

        void log(String msg) {
            System.out.println("Philosopher " + id + ": " + msg);
        }
    }

    record Fork(int id) { }
}
