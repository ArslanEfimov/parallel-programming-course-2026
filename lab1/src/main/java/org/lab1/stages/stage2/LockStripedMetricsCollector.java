package org.lab1.stages.stage2;

import org.lab1.shared.core.MetricsCollector;
import org.lab1.shared.core.Snapshot;
import org.lab1.shared.utils.PercentileCalculator;

import java.util.concurrent.atomic.AtomicLong;

public class LockStripedMetricsCollector implements MetricsCollector {
    private static final int STRIPES_COUNT = 16;

    private final long[] buckets = new long[BUCKETS_COUNT];

    private final Object[] locks = new Object[STRIPES_COUNT];
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong sum = new AtomicLong(0);
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

    public LockStripedMetricsCollector(){
        for (int stripe = 0; stripe < STRIPES_COUNT; stripe++) {
            locks[stripe] = new Object();
        }
    }
    @Override
    public void record(long value) {
        int bucket = (int) Math.min(value / 4, BUCKETS_COUNT - 1);
        int stripe = bucket % STRIPES_COUNT;
        synchronized (locks[stripe]){
            buckets[bucket]++;
        }

        count.incrementAndGet();
        sum.addAndGet(value);
        updateMin(value);
        updateMax(value);
    }

    @Override
    public Snapshot snapshot() {
        long[] copyOfBuckets = new long[BUCKETS_COUNT];
        for (int stripe = 0; stripe < STRIPES_COUNT; stripe++) {
            synchronized (locks[stripe]){
                for (int bucket = stripe; bucket < BUCKETS_COUNT; bucket+=STRIPES_COUNT) {
                    copyOfBuckets[bucket] = buckets[bucket];
                }
            }
        }
        long copyCount = count.get();
        long copySum = sum.get();
        long copyMin = min.get();
        long copyMax = max.get();

        long p50 = PercentileCalculator.percentile(copyOfBuckets, copyCount, 0.50);
        long p99 = PercentileCalculator.percentile(copyOfBuckets, copyCount, 0.99);

        return new Snapshot(
                copyOfBuckets,
                copyCount,
                copySum,
                copyMin,
                copyMax,
                p50,
                p99
        );
    }

    private void updateMin(long value){
        long currentMin = min.get();
        while(value < currentMin){
            if(min.compareAndSet(currentMin, value)) return;
            currentMin = min.get();
        }
    }

    private void updateMax(long value){
        long currentMax = max.get();
        while(value > currentMax){
            if(max.compareAndSet(currentMax, value)) return;
            currentMax = max.get();
        }
    }
}
