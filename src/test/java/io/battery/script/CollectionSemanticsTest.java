package io.battery.script;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit-test")
public class CollectionSemanticsTest {
    private static Map<String, Object> execute(String script) {
        return new BatteryScript().execute(script);
    }

    @Test
    public void givenListPlus_expectNewListAndOriginalUnchanged() {
        Map<String, Object> vars = execute("""
                l = L[1, 2];
                m = l + 3;
                """);

        // List literals are immutable, so this previously failed
        Assertions.assertThat(vars.get("l")).isEqualTo(List.of(1, 2));
        Assertions.assertThat(vars.get("m")).isEqualTo(List.of(1, 2, 3));
    }

    @Test
    public void givenSetPlus_expectNoAliasing() {
        Map<String, Object> vars = execute("""
                s = S[1];
                t = s + 2;
                a = 2 in s;
                b = 2 in t;
                """);

        Assertions.assertThat(vars)
                .containsEntry("a", false)
                .containsEntry("b", true);
    }

    @Test
    public void givenNullElements_expectIteration() {
        Map<String, Object> vars = execute("""
                listCount = 0;
                foreach (L[1, null, 3]) {
                    listCount = listCount + 1;
                }
                arrayCount = 0;
                foreach ([1, null]) {
                    arrayCount = arrayCount + 1;
                }
                joined = [1, null] + [2];
                hasNull = null in L[1, null];
                """);

        Assertions.assertThat(vars)
                .containsEntry("listCount", 3)
                .containsEntry("arrayCount", 2)
                .containsEntry("hasNull", true);
        Assertions.assertThat((Object[]) vars.get("joined")).containsExactly(1, null, 2);
    }

    @Test
    public void givenArrayEquality_expectFalseForNonArrays() {
        Map<String, Object> vars = execute("""
                a = [1] == 1;
                b = [1, 2] == [1, 2];
                c = [1, 2] == [2, 1];
                """);

        Assertions.assertThat(vars)
                .containsEntry("a", false)
                .containsEntry("b", true)
                .containsEntry("c", false);
    }
}
