package io.battery.util;

import java.util.Map;
import java.util.Objects;

import org.springframework.util.PropertyPlaceholderHelper;

/**
 * Expands placeholders in SQL statements before they run: {@code ${name}} with the value of a
 * state variable, and {@code #{expression}} with the result of a script expression. A
 * {@code ${name}} without a matching variable is left as written.
 */
public abstract class PlaceholderUtils {
    private PlaceholderUtils() {
    }

    public static String expandPlaceholders(String content,
                                            Map<String, Object> map) {
        // Unknown names resolve to null, which leaves the placeholder as written
        return new PropertyPlaceholderHelper("${", "}")
                .replacePlaceholders(content, placeholderName -> map.containsKey(placeholderName)
                        ? Objects.toString(map.get(placeholderName))
                        : null);
    }

    public static String expandEmbeddings(String content,
                                          PropertyPlaceholderHelper.PlaceholderResolver placeholderResolver) {
        return new PropertyPlaceholderHelper("#{", "}")
                .replacePlaceholders(content, placeholderResolver);
    }
}
