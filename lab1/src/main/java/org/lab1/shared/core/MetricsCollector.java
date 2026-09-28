package org.lab1.shared.core;

public interface MetricsCollector {
    void record(long value);
    Snapshot snapshot();
}
