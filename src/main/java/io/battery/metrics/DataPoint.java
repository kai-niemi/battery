package io.battery.metrics;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Values sampled at one instant, keyed for example by meter name or worker id. A sequence of
 * data points makes up the columns of a time series chart.
 *
 * @param <K> the key of each value
 * @param <V> the sampled value
 */
public abstract class DataPoint<K, V> {
    private final Instant instant;

    private final Map<K, V> metrics = new LinkedHashMap<>();

    public DataPoint(Instant instant) {
        this.instant = instant;
    }

    public Instant getInstant() {
        return instant;
    }

    public void putValue(K id, V metric) {
        metrics.put(id, metric);
    }

    public V getValue(K id, V defaultValue) {
        return metrics.getOrDefault(id, defaultValue);
    }
}
