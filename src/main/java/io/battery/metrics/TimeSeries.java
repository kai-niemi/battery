package io.battery.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;

/**
 * A rolling window of samples of selected Micrometer meters, such as connection pool or thread
 * pool meters, for charting. Each {@link #takeSnapshot() snapshot} adds a data point and drops
 * those older than the sample period, which is 5 minutes by default. Meters that aren't
 * registered yet are looked up again on each snapshot.
 * <p>
 * {@link #getDataPoints()} returns the data as columns: first the sample times in epoch
 * milliseconds, then one column of values per meter, or per statistic for meters such as
 * timers that have several.
 */
public class TimeSeries {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<DoubleDataPoint> dataPoints = Collections.synchronizedList(new ArrayList<>());

    private final List<Meter.Id> availableMeters = Collections.synchronizedList(new ArrayList<>());

    // Labels of the sampled columns by column key, in the order first sampled
    private final Map<String, String> columns = Collections.synchronizedMap(new LinkedHashMap<>());

    private Duration samplePeriod = Duration.ofSeconds(300);

    private final String name;

    private final MeterRegistry meterRegistry;

    private final List<Pair<String, Search>> pendingMeters = new ArrayList<>();

    public TimeSeries(String name,
                      MeterRegistry meterRegistry,
                      Supplier<List<Pair<String, Search>>> searchSupplier) {
        this.name = name;
        this.meterRegistry = meterRegistry;
        this.pendingMeters.addAll(searchSupplier.get());
    }

    public void registerMeters() {
        this.pendingMeters.removeIf(pair -> {
            Meter meter = pair.getSecond().meter();
            if (meter == null) {
                logger.trace("Meter '%s' for '%s' was not found!".formatted(pair.getFirst(), name));
                return false;
            } else {
                availableMeters.add(meter.getId());
                logger.debug("Meter '%s' found!".formatted(meter.getId()));
                return true;
            }
        });
    }

    public void setSamplePeriod(Duration samplePeriod) {
        this.samplePeriod = samplePeriod;
    }

    public void takeSnapshot() {
        registerMeters();

        // Purge old data points older than sample period
        dataPoints.removeIf(dataPoint -> dataPoint.getInstant()
                .isBefore(Instant.now().minusSeconds(samplePeriod.toSeconds())));

        // Add new datapoint by sampling all defined metrics
        DoubleDataPoint dataPoint = new DoubleDataPoint(Instant.now());

        meterRegistry.getMeters()
                .stream()
                .filter(meter -> availableMeters.contains(meter.getId()))
                .forEach(meter -> {
                    final Meter.Id id = meter.getId();
                    final List<Measurement> measurements = new ArrayList<>();
                    meter.measure().forEach(measurements::add);

                    // One column per measurement, so that meters with several statistics
                    // (like a timer's count, total and max) don't overwrite each other
                    final boolean multiple = measurements.size() > 1;
                    measurements.forEach(measurement -> {
                        String key = columnKey(id, measurement, multiple);
                        columns.putIfAbsent(key, columnLabel(id, measurement, multiple));
                        dataPoint.putValue(key, measurement.getValue());
                    });
                });

        dataPoints.add(dataPoint);
    }

    /**
     * @return a key unique to the meter, including its tags so that meters sharing a name
     * don't collide, and the statistic if the meter has several measurements
     */
    private static String columnKey(Meter.Id id, Measurement measurement, boolean multiple) {
        StringBuilder key = new StringBuilder(id.getName());
        id.getTags().forEach(tag -> key.append(',').append(tag.getKey()).append('=').append(tag.getValue()));
        if (multiple) {
            key.append(':').append(measurement.getStatistic().getTagValueRepresentation());
        }
        return key.toString();
    }

    private static String columnLabel(Meter.Id id, Measurement measurement, boolean multiple) {
        String label = Objects.requireNonNullElse(id.getDescription(), id.getName());
        return multiple
                ? "%s (%s)".formatted(label, measurement.getStatistic().getTagValueRepresentation())
                : label;
    }

    public List<Map<String, Object>> getDataPoints() {
        // Snapshot to avoid racing with concurrent snapshots
        final List<DoubleDataPoint> dataPoints = new ArrayList<>(this.dataPoints);
        final Map<String, String> columns;

        synchronized (this.columns) {
            columns = new LinkedHashMap<>(this.columns);
        }

        final List<Map<String, Object>> columnData = new ArrayList<>();

        {
            List<Long> labels = dataPoints.stream()
                    .map(dataPoint -> dataPoint.getInstant().toEpochMilli())
                    .toList();

            Map<String, Object> headerElement = new HashMap<>();
            headerElement.put("data", labels.toArray());

            columnData.add(headerElement);
        }

        columns.forEach((key, label) -> {
            List<Double> data = dataPoints
                    .stream()
                    .map(dataPoint -> dataPoint.getValue(key, .0))
                    .toList();

            Map<String, Object> dataElement = new HashMap<>();
            dataElement.put("data", data.toArray());
            dataElement.put("id", key);
            dataElement.put("name", label);

            columnData.add(dataElement);
        });

        return columnData;
    }
}
