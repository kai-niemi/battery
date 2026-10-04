package io.battery.util;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class PlaceholderUtilsTest {
    @Test
    public void testExpandPlaceholders() {
        String template = "Hello ${name}, your balance is ${balance}. Missing: ${missing}";
        Map<String, Object> values = Map.of("name", "Alice", "balance", 100);

        String result = PlaceholderUtils.expandPlaceholders(template, values);
        // Previously "{missing}", dropping the $
        assertThat(result).isEqualTo("Hello Alice, your balance is 100. Missing: ${missing}");
    }

    @Test
    public void testExpandPlaceholderWithNullValue() {
        Map<String, Object> values = new java.util.HashMap<>();
        values.put("name", null);

        assertThat(PlaceholderUtils.expandPlaceholders("x = ${name}", values)).isEqualTo("x = null");
    }

    @Test
    public void testExpandEmbeddings() {
        String template = "Select from #{table} where id = #{id}";
        String result = PlaceholderUtils.expandEmbeddings(template, name -> {
            if ("table".equals(name)) {
                return "users";
            }
            if ("id".equals(name)) {
                return "42";
            }
            return null;
        });

        assertThat(result).isEqualTo("Select from users where id = 42");
    }
}
