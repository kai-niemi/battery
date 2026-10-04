package io.battery.script;

import java.math.BigDecimal;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit-test")
public class NumericSemanticsTest {
    private static Map<String, Object> execute(String script) {
        return new BatteryScript().execute(script);
    }

    @Test
    public void givenMixedIntegerAndDouble_expectSymmetricPromotionToDouble() {
        Map<String, Object> vars = execute("""
                a = 1 + 2.5;
                b = 2.5 + 1;
                c = 7 / 2.0;
                d = 7 / 2;
                """);

        Assertions.assertThat(vars)
                .containsEntry("a", 3.5)
                .containsEntry("b", 3.5)
                .containsEntry("c", 3.5)
                // Integer division still truncates
                .containsEntry("d", 3);
    }

    @Test
    public void givenMixedComparisons_expectComparisonOfPromotedValues() {
        Map<String, Object> vars = execute("""
                a = 2 == 2.5;
                b = 2 < 2.5;
                c = 2 == 2.0;
                big = java.lang.Long.MAX_VALUE;
                d = 1 < big;
                e = 2 == 2.7bd;
                """);

        Assertions.assertThat(vars)
                .containsEntry("a", false)
                .containsEntry("b", true)
                .containsEntry("c", true)
                .containsEntry("d", true)
                .containsEntry("e", false);
    }

    @Test
    public void givenBigDecimalDivision_expectDecimal128Precision() {
        Map<String, Object> vars = execute("""
                a = 1bd / 3bd;
                b = 10bd / 4bd;
                c = 1.00bd / 3;
                """);

        BigDecimal third = new BigDecimal("0.3333333333333333333333333333333333");
        Assertions.assertThat(vars.get("a")).isEqualTo(third);
        Assertions.assertThat((BigDecimal) vars.get("b")).isEqualByComparingTo("2.5");
        Assertions.assertThat((BigDecimal) vars.get("c")).isEqualByComparingTo(third);
    }

    @Test
    public void givenBigDecimalEquality_expectNumericIgnoringScale() {
        Map<String, Object> vars = execute("""
                a = 1.0bd == 1.00bd;
                b = 2.0bd == 2;
                c = 2 == 2.0bd;
                d = 2.0bd >= 2 and 2.0bd <= 2;
                """);

        Assertions.assertThat(vars)
                .containsEntry("a", true)
                .containsEntry("b", true)
                .containsEntry("c", true)
                .containsEntry("d", true);
    }

    @Test
    public void givenDoubleAndBigDecimal_expectDecimalConversionAndPromotion() {
        Map<String, Object> vars = execute("""
                a = 0.1bd + 0.1;
                b = 1.5 + 1bd;
                c = 0.1bd == 0.1;
                """);

        Assertions.assertThat((BigDecimal) vars.get("a")).isEqualByComparingTo("0.2");
        Assertions.assertThat((BigDecimal) vars.get("b")).isEqualByComparingTo("2.5");
        Assertions.assertThat(vars).containsEntry("c", true);
    }

    @Test
    public void givenNamespaceOperand_expectPromotion() {
        Map<String, Object> vars = execute("""
                a = 1.0 + java.lang.Math.PI;
                b = 1 + java.lang.Math.PI;
                """);

        Assertions.assertThat(vars.get("a")).isEqualTo(1.0 + Math.PI);
        // Promoted rather than truncating PI to an integer
        Assertions.assertThat(vars.get("b")).isEqualTo(1 + Math.PI);
    }

    @Test
    public void givenBigDecimalPowers_expectIntegralExponents() {
        Map<String, Object> vars = execute("""
                a = 2bd ^ 10;
                b = 2bd ^ -1;
                """);

        Assertions.assertThat((BigDecimal) vars.get("a")).isEqualByComparingTo("1024");
        Assertions.assertThat((BigDecimal) vars.get("b")).isEqualByComparingTo("0.5");

        Assertions.assertThatThrownBy(() -> execute("a = 2bd ^ 0.5;"))
                .isInstanceOf(BatteryScriptException.class)
                .hasMessageStartingWith("Operator");
    }

    @Test
    public void givenNullComparisons_expectNullOnlyEqualToNull() {
        Map<String, Object> vars = execute("""
                _n = null;
                a = null == null;
                b = 1 == null;
                c = null != 1;
                d = "x" == _n;
                """);

        Assertions.assertThat(vars)
                .containsEntry("a", true)
                .containsEntry("b", false)
                .containsEntry("c", true)
                .containsEntry("d", false);
    }

    @Test
    public void givenNullAssignment_expectNullGlobalVariable() {
        Map<String, Object> vars = execute("n = null;");

        Assertions.assertThat(vars).containsEntry("n", null);
    }

    @Test
    public void givenNullOperand_expectScriptErrorRatherThanNullPointer() {
        Assertions.assertThatThrownBy(() -> execute("a = 1 + null;"))
                .isInstanceOf(BatteryScriptException.class)
                .hasMessageStartingWith("Operator '+' cannot be applied to 'Integer', 'null'");

        Assertions.assertThatThrownBy(() -> execute("a = null < 1;"))
                .isInstanceOf(BatteryScriptException.class)
                .hasMessageStartingWith("Operator '<' cannot be applied to 'null', 'Integer'");
    }

    @Test
    public void givenStringOperands_expectUnchangedSemantics() {
        Map<String, Object> vars = execute("""
                a = "1" + 1;
                b = 1 + "2";
                c = 1 - "2";
                d = 1.5 - "1";
                """);

        Assertions.assertThat(vars)
                .containsEntry("a", "11")
                .containsEntry("b", "12")
                .containsEntry("c", -1)
                .containsEntry("d", 0.5);
    }
}
