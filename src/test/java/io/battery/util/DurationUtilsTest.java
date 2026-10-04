package io.battery.util;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class DurationUtilsTest {
    @Test
    public void parseDurationExpressions() {
        System.out.println(LocalDateTime.now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ")));

        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSX").format(new Date()));

        assertThat(DurationUtils.parseDuration("30s")).isEqualTo(Duration.ofSeconds(30));
        assertThat(DurationUtils.parseDuration("30m")).isEqualTo(Duration.ofMinutes(30));
        assertThat(DurationUtils.parseDuration("30h")).isEqualTo(Duration.ofHours(30));
        assertThat(DurationUtils.parseDuration("30d")).isEqualTo(Duration.ofDays(30));
        assertThat(DurationUtils.parseDuration("2w")).isEqualTo(Duration.ofDays(14));

        assertThat(DurationUtils.parseDuration("45")).isEqualTo(Duration.ofSeconds(45));

        assertThat(DurationUtils.parseDuration("10m30s"))
                .isEqualTo(Duration.ofMinutes(10).plus(Duration.ofSeconds(30)));
        assertThat(DurationUtils.parseDuration("10h3m15s"))
                .isEqualTo(Duration.ofHours(10).plus(Duration.ofMinutes(3).plus(Duration.ofSeconds(15))));
        assertThat(DurationUtils.parseDuration("10h 3m 15s"))
                .isEqualTo(Duration.ofHours(10).plus(Duration.ofMinutes(3).plus(Duration.ofSeconds(15))));
    }

    @Test
    public void testDurationToDisplayString() {
        assertThat(DurationUtils.durationToDisplayString(Duration.ofMillis(500))).isEqualTo("500ms");
        assertThat(DurationUtils.durationToDisplayString(Duration.ofMillis(1500))).isEqualTo("1.50s");
        assertThat(DurationUtils.durationToDisplayString(Duration.ofMinutes(2))).isEqualTo("2m");
        assertThat(DurationUtils.durationToDisplayString(Duration.ofSeconds(150))).isEqualTo("2m30.00s");
        assertThat(DurationUtils.durationToDisplayString(Duration.ofHours(3))).isEqualTo("3h");
        assertThat(DurationUtils.durationToDisplayString(Duration.ofHours(3).plus(Duration.ofMinutes(15)))).isEqualTo(
                "3h15m");
    }
}
