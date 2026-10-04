package io.battery.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.battery.util.RandomData.selectRandom;
import static io.battery.util.RandomData.selectRandomUnique;
import static io.battery.util.RandomData.selectRandomWeighted;

@Tag("unit-test")
public class RandomDistributionTest {
    @Test
    public void testWeightedDistribution2() {
        List<Integer> data = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<Double> weights = Arrays.asList(0.9, 0.12, 0.13, 0.14, 0.15, 0.16, 0.17, 0.18, 0.19, 1.0);

        Map<Integer, AtomicInteger> hits = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            hits.computeIfAbsent(selectRandomWeighted(data, weights), k -> new AtomicInteger()).incrementAndGet();
        }

        System.out.println(hits);
    }

    @Test
    public void testWeightedDistribution3() {
        List<Integer> data = Arrays.asList(1, 2, 3);
        List<Double> weights = Arrays.asList(0.1, 1.0, 0.2);

        Map<Integer, AtomicInteger> hits = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            hits.computeIfAbsent(selectRandomWeighted(data, weights), k -> new AtomicInteger()).incrementAndGet();
        }

        System.out.println(hits);
    }

    @Test
    public void testDistribution1() {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Map<Integer, AtomicInteger> hits = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            hits.computeIfAbsent(selectRandom(data), k -> new AtomicInteger()).incrementAndGet();
        }

        System.out.println(hits);
    }

    @Test
    public void testDistribution2() {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Map<Integer, AtomicInteger> hits = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            selectRandomUnique(data, 10).forEach((k) -> {
                hits.computeIfAbsent(k, v -> new AtomicInteger()).incrementAndGet();
            });
        }

        System.out.println(hits);
    }
}
