package org.lab1.stages.stage4;

import org.lab1.shared.core.MetricsCollector;
import org.lab1.shared.core.Snapshot;
import org.lab1.shared.utils.PercentileCalculator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DoubleBufferingMetricsCollector implements MetricsCollector {

    private static final int NOWHERE = -1;
    private static final class ThreadBuffer{
        final long[][] buckets = new long[2][BUCKETS_COUNT];
        final long[] count = new long[2];
        final long[] sum = new long[2];
        final long[] min = {Long.MAX_VALUE, Long.MAX_VALUE};
        final long[] max = {Long.MIN_VALUE, Long.MIN_VALUE};

        final AtomicInteger inside = new AtomicInteger(NOWHERE);
    }

    private final AtomicInteger active = new AtomicInteger(0);

    private final Object snapLock = new Object();
    private final List<ThreadBuffer> allBuffers = new ArrayList<>();

    private final long[] globalBuckets = new long[BUCKETS_COUNT];
    private long globalCount = 0;
    private long globalSum = 0;
    private long globalMin = Long.MAX_VALUE;
    private long globalMax = Long.MIN_VALUE;

    private final ThreadLocal<ThreadBuffer> myBuffers = ThreadLocal.withInitial(() ->{
        ThreadBuffer buffer = new ThreadBuffer();
        synchronized (snapLock){
            allBuffers.add(buffer);
        }
        return buffer;
    });

    @Override
    public void record(long value) {
        ThreadBuffer myBuffer = myBuffers.get();
        int buffer;
        while (true) {
            buffer = active.get();
            myBuffer.inside.set(buffer);
//            if(active.get() == buffer){
//                break;
//            }
            myBuffer.inside.set(NOWHERE);
            break;
        }
        try {
            int bucket = (int) Math.min(value / 4, BUCKETS_COUNT - 1);
            myBuffer.buckets[buffer][bucket]++;
            myBuffer.count[buffer]++;
            myBuffer.sum[buffer] += value;
            myBuffer.min[buffer] = Math.min(myBuffer.min[buffer], value);
            myBuffer.max[buffer] = Math.max(myBuffer.max[buffer], value);
        }
        finally {
            myBuffer.inside.set(NOWHERE);
        }
    }

    @Override
    public Snapshot snapshot() {
        synchronized (snapLock){
            int old = active.get();
            active.set(1 - old);
            for (ThreadBuffer buffer : allBuffers){
                while (buffer.inside.get() == old){
                    Thread.onSpinWait();
                }
                globalCount += buffer.count[old];
                globalSum += buffer.sum[old];
                globalMin = Math.min(buffer.min[old], globalMin);
                globalMax = Math.max(buffer.max[old], globalMax);
                for (int bucket = 0; bucket < BUCKETS_COUNT; bucket++) {
                    globalBuckets[bucket] += buffer.buckets[old][bucket];
                }
                Arrays.fill(buffer.buckets[old], 0);
                buffer.count[old] = 0;
                buffer.sum[old] = 0;
                buffer.min[old] = Long.MAX_VALUE;
                buffer.max[old] = Long.MIN_VALUE;
            }

            long[] copyOfBuckets = globalBuckets.clone();
            long p50 = PercentileCalculator.percentile(copyOfBuckets, globalCount, 0.50);
            long p99 = PercentileCalculator.percentile(copyOfBuckets, globalCount, 0.99);

            return new Snapshot(
                    copyOfBuckets,
                    globalCount,
                    globalSum,
                    globalMin,
                    globalMax,
                    p50,
                    p99
            );
        }
    }
}
