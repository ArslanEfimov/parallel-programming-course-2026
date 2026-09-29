package org.lab1.shared.test;

import org.lab1.shared.core.MetricsCollector;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Worker implements Runnable{
    private final MetricsCollector collector;

    private final long[] latencies;
    private final int threadIndex;
    private final CountDownLatch ready;
    private final CountDownLatch start;
    private final AtomicBoolean stop;

    private long recordCount;

    public Worker(MetricsCollector collector, long[] latencies,
                  int threadIndex, CountDownLatch ready,
                  CountDownLatch start, AtomicBoolean stop) {
        this.collector = collector;
        this.latencies = latencies;
        this.threadIndex = threadIndex;
        this.ready = ready;
        this.start = start;
        this.stop = stop;
    }

    @Override
    public void run() {
        long localCount = 0;
        int index = threadIndex * 1000;
        ready.countDown();
        try {
            start.await();
            while (!stop.get()) {
                collector.record(latencies[index]);
                localCount++;
                index++;
                if (index == latencies.length) {
                    index = 0;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        recordCount = localCount;
    }

    public long getRecordCount() {
        return recordCount;
    }
}
