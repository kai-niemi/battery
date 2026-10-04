package io.battery.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and formats durations. Parsing accepts strings such as {@code "1h30m"}, which combine
 * the units {@code s}, {@code m}, {@code h}, {@code d} and {@code w}, or a plain number of
 * seconds. Formatting gives strings such as {@code "1h5m"}, {@code "2m3.50s"} or {@code "250ms"}.
 */
public abstract class DurationUtils {
    private static final Pattern DURATION_PATTERN = Pattern.compile("([0-9]+)([smhdw])");

    private DurationUtils() {
    }

    public static Duration parseDuration(String duration) {
        Matcher matcher = DURATION_PATTERN.matcher(duration.toLowerCase(Locale.ENGLISH));
        Duration instant = Duration.ZERO;
        while (matcher.find()) {
            int ordinal = Integer.parseInt(matcher.group(1));
            String token = matcher.group(2);
            instant = switch (token) {
                case "s" -> instant.plus(Duration.ofSeconds(ordinal));
                case "m" -> instant.plus(Duration.ofMinutes(ordinal));
                case "h" -> instant.plus(Duration.ofHours(ordinal));
                case "d" -> instant.plus(Duration.ofDays(ordinal));
                case "w" -> instant.plus(Duration.ofDays(ordinal * 7L));
                default -> throw new IllegalArgumentException("Invalid token " + token);
            };
        }
        if (instant.equals(Duration.ZERO)) {
            return Duration.ofSeconds(Integer.parseInt(duration));
        }
        return instant;
    }

    public static String durationToDisplayString(Duration duration) {
        return millisecondsToDisplayString(duration.toMillis());
    }

    public static String millisecondsToDisplayString(long timeMillis) {
        double seconds = (timeMillis / 1000.0) % 60;
        int minutes = (int) ((timeMillis / 60000) % 60);
        int hours = (int) ((timeMillis / 3600000));

        StringBuilder sb = new StringBuilder();
        if (timeMillis < 1000) {
            sb.append(String.format(Locale.US, "%dms", timeMillis));
        } else {
            if (hours > 0) {
                sb.append(String.format("%dh", hours));
            }
            if (minutes > 0) {
                sb.append(String.format("%dm", minutes));
            }
            if (hours == 0 && seconds > 0) {
                sb.append(String.format(Locale.US, "%.2fs", seconds));
            }
        }
        return sb.toString();
    }
}
