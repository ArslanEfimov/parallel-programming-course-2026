package org.lab1.shared.test;


import org.lab1.shared.core.MetricsCollector;
import org.lab1.shared.core.Snapshot;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class InconsistencyTest {

    private static final int THREAD_COUNT = 4;
    private static final int SNAPSHOT_COUNT = 10_000;

    public static void test(MetricsCollector collector, long[] latencies) throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean(false);

        Worker[] workers = new Worker[THREAD_COUNT];
        Thread[] threads = new Thread[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            workers[i] = new Worker(
                    collector,
                    latencies,
                    i,
                    ready,
                    start,
                    stop
            );
            threads[i] = new Thread(workers[i]);
            threads[i].start();
        }

        ready.await();
        start.countDown();

        long brokenSnapshots = 0;
        long sumOfBucketsLessThanCount = 0;
        long sumOfBucketsGreaterThanCount = 0;

        for (int i = 0; i < SNAPSHOT_COUNT; i++) {
            Snapshot snapshot = collector.snapshot();
            long bucketsSum = Arrays.stream(snapshot.buckets()).sum();
            long counter = snapshot.count();

            if(bucketsSum < counter){
                brokenSnapshots++;
                sumOfBucketsLessThanCount++;
            }
            else if(bucketsSum > counter){
                brokenSnapshots++;
                sumOfBucketsGreaterThanCount++;
            }
        }

        stop.set(true);
        for (Thread thread : threads){
            thread.join();
        }
        long actualRecordCalls = 0;
        for (Worker worker : workers){
            actualRecordCalls += worker.getRecordCount();
        }

        Snapshot finalSnapshot = collector.snapshot();
        long finalCount = finalSnapshot.count();
        long totalBucketsSum = Arrays.stream(finalSnapshot.buckets()).sum();

        System.out.printf("Доля битых снимков (sum != count): %.2f%%%n\n", brokenSnapshots * 100.0 / SNAPSHOT_COUNT);
        System.out.printf("Снимок: sum < count: %d\n", sumOfBucketsLessThanCount);
        System.out.printf("Снимок: sum > count: %d\n", sumOfBucketsGreaterThanCount);
        System.out.printf("Итоговый count: %d\n", finalCount);
        System.out.printf("Итоговое число записей − число вызовов record(value) в 4 потоках: %d\n", actualRecordCalls);
        System.out.printf("Итоговая сумма записей в бакетах: %d\n", totalBucketsSum);

    }
}
