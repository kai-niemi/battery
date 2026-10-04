package io.battery.metrics;

import java.time.Instant;

/**
 * A {@link DataPoint} holding the {@link Metrics} of each running worker, keyed by worker id.
 * Metrics are copied when added, since each worker keeps updating its own instance.
 */
public class MetricsDataPoint extends DataPoint<Integer, Metrics> {
    public MetricsDataPoint(Instant instant) {
        super(instant);
    }

    @Override
    public void putValue(Integer id, Metrics metric) {
        super.putValue(id, Metrics.copy(metric));
    }
}

