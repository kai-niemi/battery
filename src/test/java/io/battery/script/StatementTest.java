package io.battery.script;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.script.foo.FooBarFunctions;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class StatementTest {
    public static BatteryScript newInstance() {
        BatteryScript batteryScript = new BatteryScript();
        batteryScript.registerStandardFunctions();
        batteryScript.putExternalVariable(FooBarFunctions.NAMESPACE, new FooBarFunctions());
        return batteryScript;
    }

    @Test
    public void givenArrayExpression_expectList() {
        Map<String, Object> result = newInstance().execute("""
                a = foobar.arrayOfStrings();
                """);
        assertThat(result).containsKey("a");
    }

    @Test
    public void givenListExpression_expectList() {
        Map<String, Object> result = newInstance().execute("""
                list = foobar.listOfStrings();
                a = list.getFirst();
                c = list.getLast();
                x = std.selectRandom(list);
                """);
        assertThat(result).containsKey("list");
        assertThat(result).containsKey("a");
        assertThat(result).containsKey("c");
        assertThat(result).containsKey("x");
        assertThat(result).hasEntrySatisfying("a", rv -> {
            assertThat(rv).isEqualTo("a");
        });
    }

    @Test
    public void givenSetExpression_expectSet() {
        Map<String, Object> result = newInstance().execute("""
                set = foobar.setOfStrings();
                """);
        assertThat(result).containsKey("set");
    }

    @Test
    public void givenMapExpression_expectList() {
        Map<String, Object> result = newInstance().execute("""
                list = foobar.listOfMaps();
                a = list.getLast();
                """);
        assertThat(result).containsKey("list");
        assertThat(result).containsKey("a");
        assertThat(result).hasEntrySatisfying("a", rv -> {
            assertThat(rv).isInstanceOf(Map.class);
        });
    }

    @Test
    public void givenCreateMethod_expectCustomInstance() {
        Map<String, Object> result = newInstance().execute("""
                t=java.time.LocalDate.now();
                t2=t.plusDays(1+1);
                """);
        assertThat(result).containsKey("t");
        assertThat(result).containsKey("t2");
        assertThat(result).hasEntrySatisfying("t2", rv -> {
            assertThat(rv).isInstanceOf(LocalDate.class);
        });
    }

    @Test
    public void givenCreateMethod_expectCustomInstance2() {
        Map<String, Object> result = newInstance().execute("""
                v=java.lang.Math.max(1,2);
                """);
        assertThat(result).containsKey("v");
    }

    @Test
    public void givenStaticConstant_expectSuccess() {
        Map<String, Object> result = newInstance().execute("""
                pi=java.lang.Math.PI;
                """);
        assertThat(result).containsEntry("pi", Math.PI);
//        assertThat(result).doesNotContainKey(Constants.LAST_RESULT_VAR);
    }

    @Test
    public void givenStaticConstantInLeftOperand_expectSuccess() {
        Map<String, Object> result = newInstance().execute("""
                x=java.lang.Math.PI * 2;
                x=java.lang.Math.PI - 2;
                x=java.lang.Math.PI + 2;
                x=java.lang.Math.PI / 2;
                x=java.lang.Math.PI > 0;
                x=java.lang.Math.PI < 0;
                x=java.lang.Math.PI <= 0;
                x=java.lang.Math.PI >= 0;
                x=java.lang.Math.PI == 0;
                x=java.lang.Math.PI != 0;
                """);
//        assertThat(result).doesNotContainKey(Constants.LAST_RESULT_VAR);
    }

    @Test
    public void givenStaticConstantInRightOperand_expectSuccess() {
        Map<String, Object> result = newInstance().execute("""
                x=2 * java.lang.Math.PI;
                x=2 - java.lang.Math.PI;
                x=2 + java.lang.Math.PI;
                x=2 / java.lang.Math.PI;
                x=0 > java.lang.Math.PI;
                x=0 < java.lang.Math.PI;
                x=0 <= java.lang.Math.PI;
                x=0 >= java.lang.Math.PI;
                x=0 == java.lang.Math.PI;
                x=0 != java.lang.Math.PI;
                """);
//        assertThat(result).doesNotContainKey(Constants.LAST_RESULT_VAR);
    }

    @Test
    public void givenStaticConstant_expectImplicitAssignment() {
        Map<String, Object> result = newInstance().execute("""
                java.lang.Math.PI;
                """);
        assertThat(result).containsEntry(Constants.LAST_RESULT_VAR, Math.PI);
    }

    @Test
    public void givenStringFormat_expectExpansion() {
        Map<String, Object> result = newInstance().execute("""
                t="Hello %s, this is your mom and you are not my baby. 1+1 is %d".formatted("Bob", 1+1);
                """);
        assertThat(result).containsKey("t");
        assertThat(result).hasEntrySatisfying("t", rv -> {
            assertThat(rv).isEqualTo("Hello Bob, this is your mom and you are not my baby. 1+1 is 2");
        });
    }

    @Test
    public void givenSimpleStatement_expectVar() {
        Map<String, Object> result = newInstance().execute("""
                1 + 2
                """);
        assertThat(result).containsKey(Constants.LAST_RESULT_VAR);
    }

    @Test
    public void givenSimpleStatement_expectSuccess() {
        Map<String, Object> result = newInstance().execute("""
                x = 1 + 2;
                y = "craig";
                """);
        assertThat(result).hasEntrySatisfying("x", x -> {
            assertThat(x).isEqualTo(3);
        });
        assertThat(result).hasEntrySatisfying("y", x -> {
            assertThat(x).isEqualTo("craig");
        });
    }

    @Test
    public void givenForEachStatement_expectSuccess() {
        Map<String, Object> result = newInstance().execute("""
                a=0;
                foreach ([0,1,2,3]) {
                    a = a + _x;
                    _b = a;
                }
                """);
        assertThat(result).hasEntrySatisfying("a", x -> {
            assertThat(x).isEqualTo(6);
        });
        assertThat(result).doesNotContainKey("_b");
    }

    @Test
    public void givenWhileStatement_expectSuccess() {
        Map<String, Object> result = newInstance().execute("""
                i = 0;
                _b = 0;
                while (i<10) {
                    i = i + 1;
                }
                """);
        assertThat(result).hasEntrySatisfying("i", x -> {
            assertThat(x).isEqualTo(10);
        });
        assertThat(result).doesNotContainKey("_b");
    }

    @Test
    public void givenBranchedIfStatement_expectSuccess() {
        BatteryScript batteryScript = newInstance();

        Map<String, Object> vars = batteryScript.execute("""
                a = 0;
                if (true) {
                    a = 1;
                    _b = 2;
                    local = 3;
                }
                c = 2;
                """);

        // Assigning an existing variable updates it, while new variables are block-local
        assertThat(vars).containsEntry("a", 1);
        assertThat(vars).containsEntry("c", 2);
        assertThat(vars).doesNotContainKey("_b");
        assertThat(vars).doesNotContainKey("local");
    }

    @Test
    public void givenNestedIfStatement_expectSuccess() {
        BatteryScript batteryScript = newInstance();

        Map<String, Object> vars = batteryScript.execute("""
                a = 0;
                b = 0;
                c = 0;
                if (true) {
                    a = 1;
                    if (a >= 1) {
                        b = 1;
                    }
                    if (a > 1) {
                        c = 1;
                    }
                }
                d = 1;
                """);

        assertThat(vars).containsEntry("a", 1);
        assertThat(vars).containsEntry("b", 1);
        assertThat(vars).containsEntry("c", 0);
        assertThat(vars).containsEntry("d", 1);
    }

    @Test
    public void givenIfElseStatement_expectSuccess() {
        BatteryScript batteryScript = newInstance();

        Map<String, Object> vars = batteryScript.execute("""
                a = 0;
                b = 0;
                if (false) {
                    a = 1;
                } else {
                    b = 1;
                }
                """);

        assertThat(vars).containsEntry("a", 0);
        assertThat(vars).containsEntry("b", 1);
    }

    @Test
    public void givenElseIfStatement_expectSuccess() {
        BatteryScript batteryScript = newInstance();

        Map<String, Object> vars = batteryScript.execute("""
                a = 0;
                b = 0;
                c = 0;
                if (false) {
                    a = 1;
                } else if (true) {
                    b = 1;
                } else {
                    c = 1;
                }
                """);

        assertThat(vars).containsEntry("a", 0);
        assertThat(vars).containsEntry("b", 1);
        assertThat(vars).containsEntry("c", 0);
    }

    @Test
    public void givenInExpression_expectSuccess() {
        Map<String, Object> rs = newInstance().execute("""
                a=2 in [0,1,2,3];
                b=4 in [0,1,2,3];
                c=4 not in [0,1,2,3];
                """);
        assertThat(rs).containsEntry("a", true);
        assertThat(rs).containsEntry("b", false);
        assertThat(rs).containsEntry("c", true);
    }
}
