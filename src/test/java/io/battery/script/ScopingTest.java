package io.battery.script;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit-test")
public class ScopingTest {
    private static Map<String, Object> execute(String script) {
        return new BatteryScript().execute(script);
    }

    @Test
    public void givenWhileLoop_expectOuterVariableUpdated() {
        Map<String, Object> vars = execute("""
                i = 0;
                while (i < 3) {
                    i = i + 1;
                    inner = i;
                }
                """);

        Assertions.assertThat(vars).containsEntry("i", 3);
        Assertions.assertThat(vars).doesNotContainKey("inner");
    }

    @Test
    public void givenForLoop_expectLoopVariableLocalAndShadowing() {
        Map<String, Object> vars = execute("""
                i = 100;
                sum = 0;
                for i from 1 to 3 {
                    sum = sum + i;
                }
                """);

        // The loop variable shadows the outer i, which is unaffected by the loop
        Assertions.assertThat(vars).containsEntry("sum", 6);
        Assertions.assertThat(vars).containsEntry("i", 100);
    }

    @Test
    public void givenNestedForeach_expectInnerLoopVariablesShadowing() {
        Map<String, Object> vars = execute("""
                pairs = 0;
                outer = 0;
                foreach ([1, 2]) {
                    foreach ([10, 20, 30]) {
                        pairs = pairs + 1;
                    }
                    outer = outer + _x;
                }
                """);

        // The outer _x is restored after the inner loop
        Assertions.assertThat(vars).containsEntry("pairs", 6);
        Assertions.assertThat(vars).containsEntry("outer", 3);
    }

    @Test
    public void givenExpressionInBlock_expectGlobalLastResult() {
        Map<String, Object> vars = execute("""
                if (true) {
                    1 + 2;
                }
                """);

        Assertions.assertThat(vars).containsEntry(Constants.LAST_RESULT_VAR, 3);
    }

    @Test
    public void givenFork_expectParentVariablesUnaffected() {
        Map<String, Object> vars = execute("""
                a = 1;
                f1 = fork {
                    a = 2;
                    b = 3;
                    return a + b;
                };
                join [f1];
                rv = f1.get();
                """);

        Assertions.assertThat(vars).containsEntry("a", 1);
        Assertions.assertThat(vars).doesNotContainKey("b");
        Assertions.assertThat(vars).containsEntry("rv", 5);
    }

    @Test
    public void givenFork_expectSnapshotOfVariablesAtForkStatement() {
        Map<String, Object> vars = execute("""
                a = 1;
                f1 = fork {
                    return a;
                };
                a = 2;
                join [f1];
                rv = f1.get();
                """);

        Assertions.assertThat(vars).containsEntry("a", 2);
        Assertions.assertThat(vars).containsEntry("rv", 1);
    }

    @RepeatedTest(20)
    public void givenConcurrentForks_expectNoInterference() {
        // Both forks use the same variable and loop variable names concurrently
        Map<String, Object> vars = execute("""
                f1 = fork {
                    sum = 0;
                    foreach ([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]) {
                        sum = sum + _x;
                    }
                    return sum;
                };
                f2 = fork {
                    sum = 0;
                    foreach ([100, 200, 300, 400, 500, 600, 700, 800, 900, 1000]) {
                        sum = sum + _x;
                    }
                    return sum;
                };
                join [f1, f2];
                rv1 = f1.get();
                rv2 = f2.get();
                """);

        Assertions.assertThat(vars).containsEntry("rv1", 55);
        Assertions.assertThat(vars).containsEntry("rv2", 5500);
        Assertions.assertThat(vars).doesNotContainKey("sum");
    }
}
