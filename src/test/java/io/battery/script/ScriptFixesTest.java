package io.battery.script;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.script.atom.AtomValue;
import io.battery.script.atom.ComparableAtom;

@Tag("unit-test")
public class ScriptFixesTest {
    @Test
    public void givenForkStatement_expectVirtualThread() {
        Map<String, Object> vars = new BatteryScript().execute("""
                f1 = fork {
                  return java.lang.Thread.currentThread().isVirtual();
                };
                join [f1];
                rv = f1.get();
                """);

        Assertions.assertThat(vars.get("rv")).isEqualTo(true);
    }

    @Test
    public void givenRuntimeScriptError_expectPositionOfOffendingToken() {
        Assertions.assertThatThrownBy(() -> new BatteryScript().execute("""
                        a = 1;
                        b = true + 1;
                        c = 2;
                        """))
                .isInstanceOfSatisfying(BatteryScriptException.class, e -> {
                    // Line of the offending operator rather than the end of input
                    Assertions.assertThat(e.getOffendingTokenOffset().getFirst()).isEqualTo(2);
                    // Not wrapped again, so the position is reported once
                    Assertions.assertThat(e.getMessage().split("at position", -1)).hasSize(2);
                });
    }

    @Test
    public void givenScriptErrorInFork_expectPositionOfOffendingToken() {
        Assertions.assertThatThrownBy(() -> new BatteryScript().execute("""
                        f1 = fork {
                          return true + 1;
                        };
                        join [f1];
                        """))
                .isInstanceOfSatisfying(BatteryScriptException.class, e ->
                        Assertions.assertThat(e.getOffendingTokenOffset().getFirst()).isEqualTo(2));
    }

    @Test
    public void givenTimeLiterals_expectChronologicalComparison() {
        Map<String, Object> vars = new BatteryScript().execute("""
                ten = {t'10:00:00'};
                eleven = {t'11:00:00'};
                lt = ten < eleven;
                gt = ten > eleven;
                le = ten <= ten;
                """);

        Assertions.assertThat(vars)
                .containsEntry("lt", true)
                .containsEntry("gt", false)
                .containsEntry("le", true);
    }

    @Test
    public void givenComparableAtoms_expectComparisonOfUnwrappedValues() {
        AtomValue epoch = AtomValue.of(Instant.EPOCH);
        AtomValue later = AtomValue.of(Instant.EPOCH.plusSeconds(1));

        Assertions.assertThat(epoch).isInstanceOf(ComparableAtom.class);
        Assertions.assertThat(epoch.isEqualTo(AtomValue.of(Instant.EPOCH))).isTrue();
        Assertions.assertThat(epoch.isLessThan(later)).isTrue();
        Assertions.assertThat(later.isGreaterThanOrEqualTo(epoch)).isTrue();
    }

    private static Object evaluate(String script) {
        return new BatteryScript().execute(script).get("r");
    }

    @Test
    public void givenSingleQuotedStrings_expectEachToEndAtItsOwnQuote() {
        Assertions.assertThat(evaluate("r = 'x' + 'y';")).isEqualTo("xy");
        Assertions.assertThat(evaluate("r = \"it's\";")).isEqualTo("it's");
        Assertions.assertThatThrownBy(() -> evaluate("r = \"abc';"))
                .isInstanceOf(BatteryScriptException.class);
    }

    @Test
    public void givenQuotedDateOrTime_expectPlainString() {
        Assertions.assertThat(evaluate("r = '10:00:00';")).isEqualTo("10:00:00");
        Assertions.assertThat(evaluate("r = '2020-01-01';")).isEqualTo("2020-01-01");
    }

    @Test
    public void givenDateAndTimeLiterals_expectValues() {
        Assertions.assertThat(evaluate("r = {t'10:00:00'} < {t'11:00:00'};")).isEqualTo(true);
        Assertions.assertThat(evaluate("r = {d'2020-01-01'} < {d'2020-01-02'};")).isEqualTo(true);
        Assertions.assertThat(evaluate("r = {d '2020-01-01'};")).isEqualTo(LocalDate.of(2020, 1, 1));
        Assertions.assertThat(evaluate("r = {T'10:00:00'};")).isEqualTo(LocalTime.of(10, 0));
        Assertions.assertThat(evaluate("r = {DT'2020-01-01 10:00:00'};"))
                .isEqualTo(LocalDateTime.of(2020, 1, 1, 10, 0));
    }

    @Test
    public void givenEscapeSequences_expectDecodedString() {
        Assertions.assertThat(evaluate("r = \"a\\nb\";")).isEqualTo("a\nb");
        Assertions.assertThat(evaluate("r = \"\\t|\\r|\\b|\\f\";")).isEqualTo("\t|\r|\b|\f");
        Assertions.assertThat(evaluate("r = 'it\\'s';")).isEqualTo("it's");
        Assertions.assertThat(evaluate("r = \"say \\\"hi\\\"\";")).isEqualTo("say \"hi\"");
        Assertions.assertThat(evaluate("r = \"c:\\\\temp\";")).isEqualTo("c:\\temp");
        Assertions.assertThat(evaluate("r = \"\\u0041\\u00e9\";")).isEqualTo("A\u00e9");
        Assertions.assertThat(evaluate("r = \"\\\\u0041\";")).isEqualTo("\\u0041");
        Assertions.assertThat(evaluate("r = \"\\101\\0\\77\";")).isEqualTo("A\0?");
    }

    @Test
    public void givenBackslashInTextBlock_expectRawString() {
        Assertions.assertThat(evaluate("r = \"\"\"\nselect '\\d+'\"\"\";")).isEqualTo("select '\\d+'");
    }

    @Test
    public void givenTextBlock_expectOuterLineBreaksDropped() {
        Assertions.assertThat(evaluate("r = \"\"\"\nline1\nline2\n\"\"\";")).isEqualTo("line1\nline2");
        Assertions.assertThat(evaluate("r = \"\"\"  \r\nline1\r\nline2\r\n\"\"\";")).isEqualTo("line1\r\nline2");
        Assertions.assertThat(evaluate("r = \"\"\"\nline1\"\"\";")).isEqualTo("line1");
    }
}
