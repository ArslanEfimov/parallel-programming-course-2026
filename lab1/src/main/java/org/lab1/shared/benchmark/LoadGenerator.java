package org.lab1.shared.benchmark;

import java.util.Random;

public class LoadGenerator {

    private static final int ARRAY_SIZE = 1_048_576;
    private static final int MAX_LATENCY = 1023;
    private static final double ALPHA = 1.15;
    private static final long SEED = 13L;

    public static long[] generate(){
        Random random = new Random(SEED);
        long[] latencies = new long[ARRAY_SIZE];
        double[] cumulativeWeights = new double[MAX_LATENCY];
        double sum = 0;

        for (int k = 1; k <= MAX_LATENCY; k++) {
            sum += 1.0 / Math.pow(k, ALPHA);
            cumulativeWeights[k - 1] = sum;
        }
        for (int i = 0; i < latencies.length; i++) {
            double randomValue = random.nextDouble() * sum;
            for (int k = 0; k < cumulativeWeights.length; k++) {
                if (randomValue <= cumulativeWeights[k]) {
                    latencies[i] = k + 1;
                    break;
                }
            }
        }
        return latencies;
    }
}
