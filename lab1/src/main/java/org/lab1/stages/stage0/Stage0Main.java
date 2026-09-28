package org.lab1.stages.stage0;

import org.lab1.shared.benchmark.Benchmark;
import org.lab1.shared.benchmark.LoadGenerator;
import org.lab1.shared.core.MetricsCollector;

public class Stage0Main {
    public static void main(String[] args) throws InterruptedException {
        long[] latencies = LoadGenerator.generate();
        MetricsCollector collector = new SingleThreadMetricsCollector();
        double median = Benchmark.measurePoint(collector, latencies, 1);
        System.out.println("Median: " + median);
    }
}
