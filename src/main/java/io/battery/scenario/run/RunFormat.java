package io.battery.scenario.run;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.battery.scenario.run.RunSummary.ErrorSummary;

/**
 * Formats run summary figures for display, such as {@code "62ms"}, {@code "1.2M"} or
 * {@code "0.35%"}.
 */
public abstract class RunFormat {
    private static final DateTimeFormatter TIME
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private RunFormat() {
    }

    /**
     * @return the instant in the local time zone, such as {@code "2026-10-03 14:03:10"}
     */
    public static String time(Instant instant) {
        return TIME.format(instant);
    }

    public static String millis(double millis) {
        if (millis >= 10_000) {
            return String.format(Locale.US, "%.0fs", millis / 1000);
        }
        if (millis >= 1000) {
            return String.format(Locale.US, "%.1fs", millis / 1000);
        }
        if (millis >= 10) {
            return String.format(Locale.US, "%.0fms", millis);
        }
        return String.format(Locale.US, "%.1fms", millis);
    }

    public static String count(long count) {
        if (count >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", count / 1_000_000.0);
        }
        if (count >= 10_000) {
            return String.format(Locale.US, "%.1fk", count / 1000.0);
        }
        return Long.toString(count);
    }

    public static String rate(double opsPerSecond) {
        return opsPerSecond >= 100
                ? String.format(Locale.US, "%,.0f", opsPerSecond)
                : String.format(Locale.US, "%.1f", opsPerSecond);
    }

    public static String percent(double ratio) {
        if (ratio == 0) {
            return "0";
        }
        double percent = ratio * 100;
        return percent >= 1
                ? String.format(Locale.US, "%.1f%%", percent)
                : String.format(Locale.US, "%.2f%%", percent);
    }

    public static String ratio(double ratio) {
        return String.format(Locale.US, "%.1fx", ratio);
    }

    /**
     * @return the error type with its SQL state, such as {@code "PSQLException [40001]"}
     */
    public static String error(ErrorSummary error) {
        return error.sqlState() != null
                ? "%s [%s]".formatted(error.type(), error.sqlState())
                : error.type();
    }
}
