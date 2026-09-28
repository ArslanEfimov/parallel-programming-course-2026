package org.lab1.shared.benchmark;

import org.lab1.shared.core.MetricsCollector;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Benchmark {


    private static double run(MetricsCollector collector, long[] latencies, int threadCount, int seconds) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean(false);
        long[] ops = new long[threadCount];
        Thread[] threads = new Thread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            int threadIndex = k;
            threads[k] = new Thread(() ->{
                long localCount = 0;
                int i = threadIndex * 1000;
                try {
                    start.await();
                    while(!stop.get()){
                        collector.record(latencies[i]);
                        localCount++;
                        i++;
                        if(i == latencies.length) i = 0;
                    }
                    ops[threadIndex] = localCount;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[k].start();
        }

        long startTime = System.nanoTime();
        start.countDown();
        TimeUnit.SECONDS.sleep(seconds);
        stop.set(true);
        long endTime = System.nanoTime();

        for (Thread thread : threads) {
            thread.join();
        }

        double elapsedTime = (endTime - startTime) / (double) TimeUnit.SECONDS.toNanos(1);
        long totalOps = Arrays.stream(ops).sum();
        return totalOps / elapsedTime;
    }

    public static double measurePoint(MetricsCollector collector, long[] latencies, int threadCount) throws InterruptedException {
        final int warmUpTime = 5;
        final int raceTime = 5;
        run(collector, latencies, threadCount, warmUpTime);
        double[] results = new double[5];
        for (int i = 0; i < results.length; i++) {
            results[i] = run(collector, latencies, threadCount, raceTime);
        }
        System.out.println("Count: " + collector.snapshot().count());
        return results[2];
    }
}
