package org.lab1.stages.stage0;

import org.lab1.shared.core.MetricsCollector;
import org.lab1.shared.core.Snapshot;
import org.lab1.shared.utils.PercentileCalculator;

public class SingleThreadMetricsCollector implements MetricsCollector {
    private final long[] buckets = new long[256];

    private long count = 0;
    private long sum = 0;
    private long min = Long.MAX_VALUE;
    private long max = Long.MIN_VALUE;

    @Override
    public void record(long value) {
        int bucket = (int) Math.min(value / 4, 255);
        buckets[bucket]++;

        count++;
        sum += value;
        min = Math.min(value, min);
        max = Math.max(value, max);
    }

    @Override
    public Snapshot snapshot() {
        long[] copyOfBuckets = buckets.clone();
        long p50 = PercentileCalculator.percentile(copyOfBuckets, count, 0.50);
        long p99 = PercentileCalculator.percentile(copyOfBuckets, count, 0.99);

        return new Snapshot(
                copyOfBuckets,
                count,
                sum,
                min,
                max,
                p50,
                p99
        );
    }

}
