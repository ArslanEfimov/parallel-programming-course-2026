package org.lab1.stages.stage1;

import org.lab1.shared.benchmark.Benchmark;
import org.lab1.shared.benchmark.LoadGenerator;
import org.lab1.shared.core.MetricsCollector;

public class Stage1Main {
    public static void main(String[] args) throws InterruptedException {
        long[] latencies = LoadGenerator.generate();
        MetricsCollector collector = new EmptySynchronizedMetricsCollector();
        double median = Benchmark.measurePoint(collector, latencies, 2);
        System.out.println("Median: " + median);
    }
}
