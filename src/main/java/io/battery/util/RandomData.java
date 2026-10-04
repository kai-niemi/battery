package io.battery.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Random selection for generating data and picking scenarios: uniform picks from enums, lists,
 * sets and arrays, picks of unique elements, and picks weighted by a list of weights. Uses
 * {@link ThreadLocalRandom}, so it can be called from concurrent virtual users.
 */
public abstract class RandomData {
    private RandomData() {
    }

    public static <T extends Enum<?>> T selectRandom(Class<T> clazz) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

    public static <E> E selectRandom(List<E> collection) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return collection.get(random.nextInt(collection.size()));
    }

    @SuppressWarnings("unchecked")
    public static <K> K selectRandom(Set<K> set) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Object[] keys = set.toArray();
        return (K) keys[random.nextInt(keys.length)];
    }

    public static <E> E selectRandom(E[] collection) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return collection[random.nextInt(collection.length)];
    }

    /**
     * @return count distinct elements picked at random
     * @throws IllegalArgumentException if the collection has fewer distinct elements
     */
    public static <E> Collection<E> selectRandomUnique(List<E> collection, int count) {
        // Pick from the distinct elements, since duplicates can't make up the count
        List<E> distinct = new ArrayList<>(new LinkedHashSet<>(collection));
        if (count > distinct.size()) {
            throw new IllegalArgumentException("Not enough unique elements: %d of %d"
                    .formatted(distinct.size(), count));
        }

        Collections.shuffle(distinct, ThreadLocalRandom.current());
        return new HashSet<>(distinct.subList(0, count));
    }

    /**
     * @return count distinct elements picked at random
     * @throws IllegalArgumentException if the array has fewer distinct elements
     */
    public static <E> Collection<E> selectRandomUnique(E[] array, int count) {
        return selectRandomUnique(Arrays.asList(array), count);
    }

    public static <T> T selectRandomWeighted(Collection<T> items, List<Double> weights) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Empty collection");
        }
        if (items.size() != weights.size()) {
            throw new IllegalArgumentException("Collection and weights mismatch");
        }

        double totalWeight = weights.stream().mapToDouble(w -> w).sum();
        double randomWeight = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulativeWeight = 0;

        int idx = 0;
        for (T item : items) {
            cumulativeWeight += weights.get(idx++);
            if (cumulativeWeight >= randomWeight) {
                return item;
            }
        }

        throw new IllegalStateException("This is not possible");
    }

    public static String wordBreaks(String text, int lineSize) {
        Pattern p = Pattern.compile("\\b.{1," + (lineSize - 1) + "}\\b\\W?");
        Matcher m = p.matcher(text);

        List<String> words = new ArrayList<>();
        while (m.find()) {
            words.add(m.group());
        }

        return String.join("\n", words);
    }
}
