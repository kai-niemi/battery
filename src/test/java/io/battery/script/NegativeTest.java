package io.battery.script;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@Tag("unit-test")
public class NegativeTest {
    public static BatteryScript newInstance() {
        BatteryScript batteryScript = new BatteryScript();
        batteryScript.registerStandardFunctions();
        return batteryScript;
    }

    @Test
    public void givenBadVariableName_expectFail() {
        assertThatExceptionOfType(BatteryScriptException.class).isThrownBy(() -> {
            newInstance().execute("""
                    k;
                    """);
        }).withMessageStartingWith("Cannot resolve symbol 'k'");
    }

    @Test
    public void givenBadVariableAssignment_expectFail() {
        assertThatExceptionOfType(BatteryScriptException.class).isThrownBy(() -> {
            newInstance().execute("""
                    k = x;
                    """);
        }).withMessageStartingWith("Cannot resolve symbol 'x'");
    }

    @Test
    public void givenBadFieldName_expectFail() {
        assertThatExceptionOfType(BatteryScriptException.class).isThrownBy(() -> {
            newInstance().execute("""
                    k = gen.randomDate().y;
                    """);
        }).withMessageStartingWith("No matching field 'y' in LocalDate");
    }

    @Test
    public void givenBadMethodName_expectFail() {
        assertThatExceptionOfType(BatteryScriptException.class).isThrownBy(() -> {
            newInstance().execute("""
                    k = gen.randomDate().y();
                    """);
        }).withMessageStartingWith("Error invoking method 'y' on 'java.time.LocalDate'");
    }

    @Test
    public void givenBadVariableInOperator_expectPropagation() {
        assertThatExceptionOfType(BatteryScriptException.class).isThrownBy(() -> {
            Map<String, Object> vars = newInstance().execute("""
                      x = b == 10;
                    """);
        }).withMessageStartingWith(
                "Cannot resolve symbol 'b'");
    }

    @Test
    public void givenBadVariableInBranch_expectPropagation() {
        assertThatExceptionOfType(BatteryScriptException.class).isThrownBy(() -> {
            Map<String, Object> vars = newInstance().execute("""
                    a = 10;
                    if (b == 10) {
                       a = b + 1;
                    }
                    """);
        }).withMessageStartingWith(
                "Cannot resolve symbol 'b'");
    }

}
