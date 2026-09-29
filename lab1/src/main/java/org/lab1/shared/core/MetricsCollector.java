package org.lab1.shared.core;

public interface MetricsCollector {
    int BUCKETS_COUNT = 256;

    void record(long value);
    Snapshot snapshot();
}
