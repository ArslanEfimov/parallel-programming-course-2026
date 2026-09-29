package org.lab1.shared.utils;

public class PercentileCalculator {

    public static long percentile(long[] buckets, long count, double percentile){
        if(count == 0) return 0;

        double target = count * percentile;
        long accum = 0;

        for(int i = 0; i < buckets.length; i++){
            accum += buckets[i];

            if(accum >= target){
                return i * 4L;
            }
        }
        return 0;
    }
}
