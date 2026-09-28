package org.lab1.stages.stage2;

import org.lab1.shared.benchmark.Benchmark;
import org.lab1.shared.benchmark.LoadGenerator;
import org.lab1.shared.core.MetricsCollector;
import org.lab1.shared.test.InconsistencyTest;

public class Stage2Main {

    public static void main(String[] args) throws InterruptedException {
        long[] latencies = LoadGenerator.generate();
        MetricsCollector collector = new LockStripedMetricsCollector();
//        double median = Benchmark.measurePoint(collector, latencies, 8);
//        System.out.println("Median: " + median);

        InconsistencyTest.test(collector, latencies);
    }
}
