package org.lab1.stages.stage3;

import org.lab1.shared.core.MetricsCollector;
import org.lab1.shared.core.Snapshot;
import org.lab1.shared.utils.PercentileCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public class ThreadLocalMetricsCollector implements MetricsCollector {
    private static final class ThreadState {
        final AtomicLongArray buckets = new AtomicLongArray(BUCKETS_COUNT);
        final AtomicLong count = new AtomicLong();
        final AtomicLong sum = new AtomicLong();
        final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
    }
    private final List<ThreadState> allStates = new ArrayList<>();
    private final Object listLock = new Object();

    private final ThreadLocal<ThreadState> myState = ThreadLocal.withInitial(() -> {
        ThreadState state = new ThreadState();
        synchronized (listLock){
            allStates.add(state);
        }
        return state;
    });

    @Override
    public void record(long value) {
        ThreadState state = myState.get();
        int bucket = (int) Math.min(value / 4, BUCKETS_COUNT - 1);
        state.buckets.setRelease(bucket, state.buckets.get(bucket) + 1);
        state.count.setRelease(state.count.getPlain() + 1);
        state.sum.setRelease(state.sum.getPlain() + value);

        if(value < state.min.getPlain()) state.min.setRelease(value);
        if(value > state.max.getPlain()) state.max.setRelease(value);
    }

    @Override
    public Snapshot snapshot() {
        long[] out = new long[BUCKETS_COUNT];
        long copyCount = 0;
        long copySum = 0;
        long copyMin = Long.MAX_VALUE;
        long copyMax = Long.MIN_VALUE;

        List<ThreadState> copyOfStates;
        synchronized (listLock){
            copyOfStates = new ArrayList<>(allStates);
        }

        for (ThreadState state : copyOfStates){
            for(int i = 0; i < BUCKETS_COUNT; i++){
                out[i] += state.buckets.get(i);
            }
            copyCount += state.count.get();
            copySum += state.sum.get();
            copyMin = Math.min(copyMin, state.min.get());
            copyMax = Math.max(copyMax, state.max.get());
        }

        long p50 = PercentileCalculator.percentile(out, copyCount, 0.50);
        long p99 = PercentileCalculator.percentile(out, copyCount, 0.99);
        return new Snapshot(
                out,
                copyCount,
                copySum,
                copyMin,
                copyMax,
                p50,
                p99
        );
    }
}
