package io.battery.script;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;

import io.battery.script.atom.AtomValue;
import io.battery.script.atom.BigDecimalAtom;
import io.battery.script.atom.DoubleAtom;
import io.battery.script.function.Description;
import io.battery.script.function.EncodingFunctions;
import io.battery.script.function.GenerateFunctions;
import io.battery.script.function.NetworkFunctions;
import io.battery.script.function.StandardFunctions;
import io.battery.script.function.Volatility;

@Tag("unit-test")
public class RemainingFixesTest {
    private static Map<String, Object> execute(String script) {
        BatteryScript batteryScript = new BatteryScript();
        batteryScript.registerStandardFunctions();
        return batteryScript.execute(script);
    }

    private static int errorLine(String script) {
        try {
            execute(script);
        } catch (BatteryScriptException e) {
            return e.getOffendingTokenOffset().getFirst();
        }
        throw new AssertionError("Expected script error");
    }

    // 1. Runtime error positions

    @Test
    public void givenUnresolvedSymbol_expectPositionOfSymbol() {
        Assertions.assertThat(errorLine("""
                a = 1;
                b = c + 1;
                d = 2;
                """)).isEqualTo(2);
    }

    @Test
    public void givenDivisionByZero_expectPositionedScriptError() {
        Assertions.assertThatThrownBy(() -> execute("""
                        a = 1;
                        b = a / 0;
                        """))
                .isInstanceOf(BatteryScriptException.class)
                .hasMessageStartingWith("/ by zero at position 2:");
    }

    // 2. Top-level return

    @Test
    public void givenTopLevelReturn_expectEndOfScriptWithResult() {
        Map<String, Object> vars = execute("""
                a = 1;
                if (a == 1) {
                    return a + 1;
                }
                b = 2;
                """);

        Assertions.assertThat(vars).containsEntry(Constants.LAST_RESULT_VAR, 2);
        Assertions.assertThat(vars).doesNotContainKey("b");
    }

    // 3. Validation of unterminated scripts

    @Test
    public void givenMissingFinalSemicolon_expectValidAsForExecution() {
        new BatteryScript().validate("x = 1");
        Assertions.assertThat(execute("x = 1")).containsEntry("x", 1);
    }

    // 4. Date and time comparisons with other types

    @Test
    public void givenDateComparedWithOtherTypes_expectFalseOrScriptError() {
        Map<String, Object> vars = execute("""
                d = {d'2020-01-01'};
                a = d == "x";
                b = "2020-01-01" == d;
                c = d == {d'2020-01-01'};
                """);

        Assertions.assertThat(vars)
                .containsEntry("a", false)
                .containsEntry("b", true)
                .containsEntry("c", true);

        Assertions.assertThatThrownBy(() -> execute("d = {d'2020-01-01'}; a = d < 1;"))
                .isInstanceOf(BatteryScriptException.class)
                .hasMessageStartingWith("Operator '<' cannot be applied to 'LocalDate', 'Integer'");
    }

    // 5. Strings compared with namespaces

    @Test
    public void givenStringComparedWithNamespace_expectResolvedValue() {
        Map<String, Object> vars = execute("""
                a = "%s" == java.lang.Math.PI;
                """.formatted(Math.PI));

        Assertions.assertThat(vars).containsEntry("a", true);
    }

    // 6. Numeric edge cases

    @Test
    public void givenIntegerOverflow_expectScriptError() {
        Assertions.assertThatThrownBy(() -> execute("a = 2147483647 + 1;"))
                .isInstanceOf(BatteryScriptException.class)
                .hasMessageContaining("overflow");
        Assertions.assertThatThrownBy(() -> execute("a = 2 ^ 40;"))
                .isInstanceOf(BatteryScriptException.class)
                .hasMessageContaining("overflow");
    }

    @Test
    public void givenIntegerPowers_expectExactOrDoubleForNegativeExponent() {
        Map<String, Object> vars = execute("""
                a = 2 ^ 10;
                b = 2 ^ -1;
                c = 3 ^ 0;
                """);

        Assertions.assertThat(vars)
                .containsEntry("a", 1024)
                .containsEntry("b", 0.5)
                .containsEntry("c", 1);
    }

    @Test
    public void givenOtherNumberTypes_expectNormalizedAtoms() {
        Assertions.assertThat(AtomValue.of(0.1f)).isInstanceOf(DoubleAtom.class);
        Assertions.assertThat(AtomValue.of(0.1f).asObject()).isEqualTo(0.1);
        Assertions.assertThat(AtomValue.of((short) 2).plus(AtomValue.of(1)).asObject()).isEqualTo(3);
        Assertions.assertThat(AtomValue.of(BigInteger.TEN)).isInstanceOf(BigDecimalAtom.class);
        Assertions.assertThat(AtomValue.of(BigInteger.TEN).plus(AtomValue.of(1)).asObject())
                .isEqualTo(BigDecimal.valueOf(11));
    }

    // 8. Function metadata

    @Test
    public void givenSizeOf_expectCollectionAndArraySizes() {
        Map<String, Object> vars = execute("""
                a = std.sizeOf(L[1, 2, 3]);
                b = std.sizeOf([1, 2]);
                c = std.sizeOf(1, 2, 3, 4);
                """);

        Assertions.assertThat(vars)
                .containsEntry("a", 3)
                .containsEntry("b", 2)
                .containsEntry("c", 4);
    }

    @Test
    public void givenEncodingNamespace_expectRegistered() {
        Map<String, Object> vars = execute("""
                a = encoding.toSecureHash("x");
                """);

        Assertions.assertThat((String) vars.get("a")).isNotBlank();
    }

    @Test
    public void givenRandomIPv4_expectFullOctetRange() {
        NetworkFunctions network = new NetworkFunctions() {
        };
        boolean found255 = false;
        for (int i = 0; i < 20_000 && !found255; i++) {
            found255 = network.randomIPv4().matches("(.*\\.)?255(\\..*)?");
        }
        Assertions.assertThat(found255).isTrue();
    }

    @Test
    public void givenNonDeterministicFunctions_expectVolatile() throws Exception {
        Assertions.assertThat(volatility(GenerateFunctions.class.getMethod("randomInt", int.class, int.class)))
                .isEqualTo(Volatility.Volatile);
        Assertions.assertThat(volatility(StandardFunctions.class.getMethod("currentDate")))
                .isEqualTo(Volatility.Volatile);
        Assertions.assertThat(volatility(NetworkFunctions.class.getMethod("publicIP")))
                .isEqualTo(Volatility.Volatile);
        Assertions.assertThat(volatility(EncodingFunctions.class.getMethod("toBase64", byte[].class)))
                .isEqualTo(Volatility.Immutable);
    }

    private static Volatility volatility(java.lang.reflect.Method method) {
        return AnnotationUtils.findAnnotation(method, Description.class).volatility();
    }
}
