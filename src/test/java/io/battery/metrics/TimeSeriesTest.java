package io.battery.metrics;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Tag("unit-test")
public class TimeSeriesTest {
    @Test
    public void givenTimer_expectColumnPerStatistic() {
        MeterRegistry registry = new SimpleMeterRegistry();
        Timer timer = Timer.builder("calls").description("Calls").register(registry);
        timer.record(Duration.ofMillis(10));

        TimeSeries timeSeries = new TimeSeries("test", registry,
                () -> List.of(Pair.of("calls", registry.find("calls"))));
        timeSeries.takeSnapshot();

        List<Map<String, Object>> columns = timeSeries.getDataPoints();

        Assertions.assertThat(columns).hasSize(4);
        Assertions.assertThat(columns.subList(1, 4))
                .extracting(column -> column.get("id"))
                .containsExactly("calls:count", "calls:total", "calls:max");
        Assertions.assertThat(columns.subList(1, 4))
                .extracting(column -> column.get("name"))
                .containsExactly("Calls (count)", "Calls (total)", "Calls (max)");
        Assertions.assertThat((Object[]) columns.get(1).get("data")).containsExactly(1.0);
    }

    @Test
    public void givenGaugesSharingName_expectColumnPerGauge() {
        MeterRegistry registry = new SimpleMeterRegistry();
        registry.gauge("pool.size", List.of(io.micrometer.core.instrument.Tag.of("pool", "a")), 1);
        registry.gauge("pool.size", List.of(io.micrometer.core.instrument.Tag.of("pool", "b")), 2);

        TimeSeries timeSeries = new TimeSeries("test", registry, () -> List.of(
                Pair.of("a", registry.find("pool.size").tag("pool", "a")),
                Pair.of("b", registry.find("pool.size").tag("pool", "b"))));
        timeSeries.takeSnapshot();
        timeSeries.takeSnapshot();

        List<Map<String, Object>> columns = timeSeries.getDataPoints();

        Assertions.assertThat((Object[]) columns.getFirst().get("data")).hasSize(2);
        Assertions.assertThat(columns.subList(1, columns.size()))
                .extracting(column -> column.get("id"), column -> List.of((Object[]) column.get("data")))
                .containsExactlyInAnyOrder(
                        Assertions.tuple("pool.size,pool=a", List.of(1.0, 1.0)),
                        Assertions.tuple("pool.size,pool=b", List.of(2.0, 2.0)));
        // Falls back to the meter name without a description
        Assertions.assertThat(columns.get(1).get("name")).isEqualTo("pool.size");
    }

    @Test
    public void givenMeterRegisteredLater_expectEarlierSamplesDefaultToZero() {
        MeterRegistry registry = new SimpleMeterRegistry();
        TimeSeries timeSeries = new TimeSeries("test", registry,
                () -> List.of(Pair.of("late", registry.find("late"))));

        timeSeries.takeSnapshot();
        registry.gauge("late", 5);
        timeSeries.takeSnapshot();

        List<Map<String, Object>> columns = timeSeries.getDataPoints();

        Assertions.assertThat(columns).hasSize(2);
        Assertions.assertThat((Object[]) columns.get(1).get("data")).containsExactly(0.0, 5.0);
    }
}
