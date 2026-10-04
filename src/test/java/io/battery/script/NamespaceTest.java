package io.battery.script;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit-test")
public class NamespaceTest {
    private static Map<String, Object> execute(String script) {
        BatteryScript batteryScript = new BatteryScript();
        batteryScript.registerStandardFunctions();
        return batteryScript.execute(script);
    }

    @Test
    public void givenApplicationClass_expectResolvedFromClasspath() {
        // Previously only JDK (platform) classes were resolvable
        Map<String, Object> vars = execute("a = io.battery.script.Constants.LAST_RESULT_VAR;");

        Assertions.assertThat(vars).containsEntry("a", Constants.LAST_RESULT_VAR);
    }

    @Test
    public void givenAssignmentOfNamespaceName_expectRejected() {
        Assertions.assertThatThrownBy(() -> execute("log = \"message\";"))
                .isInstanceOf(BatteryScriptException.class)
                .hasMessageStartingWith("Cannot assign 'log', which is a registered namespace");
        Assertions.assertThatThrownBy(() -> execute("for gen from 1 to 2 { x = 1; }"))
                .isInstanceOf(BatteryScriptException.class)
                .hasMessageStartingWith("Cannot assign 'gen', which is a registered namespace");
        Assertions.assertThatThrownBy(() -> execute("std = fork { return 1; };"))
                .isInstanceOf(BatteryScriptException.class)
                .hasMessageStartingWith("Cannot assign 'std', which is a registered namespace");
    }
}
