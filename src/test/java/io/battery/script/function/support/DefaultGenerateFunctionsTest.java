package io.battery.script.function.support;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import io.battery.script.function.DefaultGenerateFunctions;

@Tag("unit-test")
public class DefaultGenerateFunctionsTest {
    private final DefaultGenerateFunctions functions = new DefaultGenerateFunctions();

    @Test
    public void givenRandomBigDecimal_expectRangeUpTo2Pow16() {
        BigDecimal max = BigDecimal.ZERO;
        for (int i = 0; i < 10_000; i++) {
            BigDecimal value = functions.randomBigDecimal();
            Assertions.assertThat(value).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(1 << 16));
            max = max.max(value);
        }
        // With the former XOR bug (2 ^ 16 = 18), values never exceeded 18
        Assertions.assertThat(max).isGreaterThan(BigDecimal.valueOf(18));
    }

    @Test
    public void givenRandomJson_expectEachUserOnce() {
        JsonNode root = JsonMapper.builder().build().readTree(functions.randomJson(3, 2));

        Assertions.assertThat(root.get("users").size()).isEqualTo(3);
        root.get("users").forEach(user ->
                Assertions.assertThat(user.get("addresses").size()).isEqualTo(2));
    }
}
