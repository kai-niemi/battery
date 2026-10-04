package io.battery.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

import org.springframework.data.util.Pair;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Call statistics for one worker (virtual user): success and failure counts since the worker
 * started, and throughput, mean latency and latency percentiles over the last 10 seconds.
 * Latencies are in milliseconds.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Metrics {
    private static final int MAX_AGE_SECONDS = 10;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Metrics instance = new Metrics();

        private Builder() {
        }

        public Builder withUpdateTime(Instant updateTime) {
            instance.updateTime = updateTime;
            return this;
        }

        public Builder withMeanTimeMillis(double meanTimeMillis) {
            instance.meanTimeMillis = meanTimeMillis;
            return this;
        }

        public Builder withOps(double opsPerSec, double opsPerMin) {
            instance.opsPerSec = opsPerSec;
            instance.opsPerMin = opsPerMin;
            return this;
        }

        public Builder withP50(double p50) {
            instance.p50 = p50;
            return this;
        }

        public Builder withP90(double p90) {
            instance.p90 = p90;
            return this;
        }

        public Builder withP95(double p95) {
            instance.p95 = p95;
            return this;
        }

        public Builder withP999(double p999) {
            instance.p999 = p999;
            return this;
        }

        public Builder withP99(double p99) {
            instance.p99 = p99;
            return this;
        }

        public Builder withSuccessful(int successful) {
            instance.success = successful;
            return this;
        }

        public Builder withFails(int transientFail, int nonTransientFail) {
            instance.transientFail = transientFail;
            instance.nonTransientFail = nonTransientFail;
            return this;
        }

        public Metrics build() {
            Assert.notNull(instance.updateTime, "time is null");
            return instance;
        }
    }

    public static Metrics empty() {
        return new Metrics();
    }

    /**
     * @return a copy of all statistics, but not of the samples behind them, so the copy
     * reflects the statistics at the time of copying
     */
    public static Metrics copy(Metrics from) {
        Metrics m = new Metrics();
        m.updateTime = from.updateTime;
        m.success = from.success;
        m.transientFail = from.transientFail;
        m.nonTransientFail = from.nonTransientFail;
        m.opsPerSec = from.opsPerSec;
        m.opsPerMin = from.opsPerMin;
        m.meanTimeMillis = from.meanTimeMillis;
        m.p50 = from.p50;
        m.p90 = from.p90;
        m.p95 = from.p95;
        m.p99 = from.p99;
        m.p999 = from.p999;
        return m;
    }

    private static double percentile(List<Double> durations, double percentile) {
        if (percentile < 0 || percentile > 1) {
            throw new IllegalArgumentException(">=0 N <=1");
        }
        if (!durations.isEmpty()) {
            int index = (int) Math.ceil(percentile * durations.size());
            return durations.get(index - 1);
        }
        return 0;
    }

    @JsonIgnore
    private final LinkedList<Pair<Instant, Pair<Duration, Integer>>> fifoBuffer = new LinkedList<>();

    private Instant updateTime;

    private int success;

    private int transientFail;

    private int nonTransientFail;

    private double opsPerSec;

    private double opsPerMin;

    private double meanTimeMillis;

    private double p50;

    private double p90;

    private double p95;

    private double p99;

    private double p999;

    private Metrics() {
        this.updateTime = Instant.now();
    }

    public Instant getUpdateTime() {
        return updateTime;
    }

    public int getSuccess() {
        return success;
    }

    public int getTransientFail() {
        return transientFail;
    }

    public int getNonTransientFail() {
        return nonTransientFail;
    }

    public double getMeanTimeMillis() {
        return meanTimeMillis;
    }

    public double getOpsPerSec() {
        return opsPerSec;
    }

    public double getOpsPerMin() {
        return opsPerMin;
    }

    public double getP50() {
        return p50;
    }

    public double getP90() {
        return p90;
    }

    public double getP95() {
        return p95;
    }

    public double getP99() {
        return p99;
    }

    public double getP999() {
        return p999;
    }

    public void markSuccess(Duration duration) {
        success++;
        update(duration);
    }

    public void markFail(Duration duration, boolean isTransient) {
        if (isTransient) {
            transientFail++;
        } else {
            nonTransientFail++;
        }
        update(duration);
    }

    private void update(Duration duration) {
        updateTime = Instant.now();

        fifoBuffer.add(Pair.of(updateTime, Pair.of(duration, success)));

        // Purge items by time range
        fifoBuffer.removeIf(item -> item.getFirst()
                .isBefore(updateTime.minusSeconds(MAX_AGE_SECONDS)));

        // Latency percentiles and mean time
        {
            final List<Double> durationMillis = fifoBuffer
                    .stream()
                    .map(p -> p.getSecond().getFirst())
                    .toList()
                    .stream()
                    .mapToDouble(Duration::toMillis)
                    .sorted()
                    .boxed()
                    .toList();

            p50 = percentile(durationMillis, .5);
            p90 = percentile(durationMillis, .9);
            p95 = percentile(durationMillis, .95);
            p99 = percentile(durationMillis, .99);
            p999 = percentile(durationMillis, .999);

            meanTimeMillis = durationMillis
                    .stream()
                    .mapToDouble(value -> value)
                    .average()
                    .orElse(0);
        }

        // Ops per time unit
        {
            List<Instant> samples = fifoBuffer
                    .stream()
                    .map(Pair::getFirst)
                    .filter(instant -> instant.isAfter(updateTime.minusSeconds(MAX_AGE_SECONDS)))
                    .toList();
            Instant oldestTime = samples.stream()
                    .findFirst()
                    .orElse(updateTime);

            opsPerSec = samples.size() /
                        Math.max(1, Duration.between(oldestTime, updateTime).toMillis() / 1000.0);
            opsPerSec = Math.round(opsPerSec * 10.0) / 10.0;

            opsPerMin = opsPerSec * 60;
        }
    }

}
