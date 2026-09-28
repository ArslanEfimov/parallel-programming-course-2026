package org.lab1.stages.stage1;

import org.lab1.shared.core.MetricsCollector;
import org.lab1.shared.core.Snapshot;

public class EmptySynchronizedMetricsCollector implements MetricsCollector {
    private final long[] buckets = new long[256];

    private long count = 0;
    private long sum = 0;

    @Override
    public synchronized void record(long value) {

    }

    @Override
    public Snapshot snapshot() {
        long[] copyOfBuckets = buckets.clone();
        return new Snapshot(
                copyOfBuckets,
                count,
                sum,
                0,
                0,
                0,
                0
        );
    }
}
