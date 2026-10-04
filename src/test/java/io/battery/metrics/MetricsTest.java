package io.battery.metrics;

import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit-test")
public class MetricsTest {
    @Test
    public void givenMetrics_expectCopyOfAllStatistics() {
        Metrics metrics = Metrics.empty();
        for (int i = 1; i <= 100; i++) {
            metrics.markSuccess(Duration.ofMillis(i));
        }
        metrics.markFail(Duration.ofMillis(5), true);
        metrics.markFail(Duration.ofMillis(5), false);

        Metrics copy = Metrics.copy(metrics);

        Assertions.assertThat(copy.getP90()).isGreaterThan(0);
        Assertions.assertThat(copy.getOpsPerSec()).isGreaterThan(0);
        Assertions.assertThat(copy)
                .usingRecursiveComparison()
                .ignoringFields("fifoBuffer")
                .isEqualTo(metrics);
    }

    @Test
    public void givenCopy_expectUnaffectedByLaterUpdates() {
        Metrics metrics = Metrics.empty();
        metrics.markSuccess(Duration.ofMillis(10));

        Metrics copy = Metrics.copy(metrics);
        metrics.markSuccess(Duration.ofMillis(20));

        Assertions.assertThat(copy.getSuccess()).isEqualTo(1);
        Assertions.assertThat(metrics.getSuccess()).isEqualTo(2);
    }
}
