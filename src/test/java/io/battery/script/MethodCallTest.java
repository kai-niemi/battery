package io.battery.script;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.script.foo.FooBarFunctions;

@Tag("unit-test")
public class MethodCallTest {
    public static BatteryScript mockInstance() {
        BatteryScript batteryScript = new BatteryScript();
        batteryScript.registerStandardFunctions();
        batteryScript.putExternalVariable("foobar", new FooBarFunctions());
        return batteryScript;
    }

    @Test
    public void givenSimpleExpressions_expectSuccess() {
        mockInstance().execute("""
                x = 1 + 2;
                y = "craig";
                log.info (\"Hello world\");
                log.warn (\"Hello world\");
                log.error (\"Hello world\");
                log.debug (\"Hello world\");
                log.debug (\"Hello 1+2 is %d says %s\", [x,y]);
                """);
    }

    @Test
    public void givenRandomInt_expectNumber() {
        mockInstance().execute("""
                a = gen.randomInt(1,200);
                """);
    }

    @Test
    public void givenNestedCustomFunctions1_expectSuccess() {
        BatteryScript batteryScript = mockInstance();

        Map<String, Object> vars = batteryScript.execute("""
                f = foobar.foo();
                b = f.bar();
                t = b.toString();
                log.info (t);
                """);

        Assertions.assertTrue(vars.containsKey("f"));
        Assertions.assertTrue(vars.containsKey("b"));
        Assertions.assertTrue(vars.containsKey("t"));
    }

    @Test
    public void givenNestedCustomFunctions2_expectSuccess() {
        BatteryScript batteryScript = mockInstance();

        Map<String, Object> vars = batteryScript.execute("""
                b = foobar.foo().bar();
                t = b.toString();
                log.info (t);
                """);

        Assertions.assertTrue(vars.containsKey("b"));
        Assertions.assertTrue(vars.containsKey("t"));
    }

    @Test
    public void givenNestedCustomFunctions3_expectSuccess() {
        BatteryScript batteryScript = mockInstance();

        Map<String, Object> vars = batteryScript.execute("""
                t = foobar.foo().bar().toString();
                log.info (t);
                """);

        Assertions.assertTrue(vars.containsKey("t"));
    }

    @Test
    public void givenNestedCustomFunctions4_expectSuccess() {
        BatteryScript batteryScript = mockInstance();

        Map<String, Object> vars = batteryScript.execute("""
                f = foobar.foo();
                h = f.hello;
                """);

        Assertions.assertTrue(vars.containsKey("f"));
        Assertions.assertTrue(vars.containsKey("h"));
    }

    @Test
    public void givenNestedCustomFunctions5_expectSuccess() {
        BatteryScript batteryScript = mockInstance();

        Map<String, Object> vars = batteryScript.execute("""
                a = [1,2,3,4,5];
                l = std.sizeOf(a);
                """);

        Assertions.assertTrue(vars.containsKey("a"));
        Assertions.assertTrue(vars.containsKey("l"));
        Assertions.assertEquals(5, vars.get("l"));
    }
}
