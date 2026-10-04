package io.battery.metrics;

import java.time.Instant;

/**
 * A {@link DataPoint} holding one sampled value per meter name, as used by {@link TimeSeries}.
 */
public class DoubleDataPoint extends DataPoint<String, Double> {
    public DoubleDataPoint(Instant instant) {
        super(instant);
    }
}

