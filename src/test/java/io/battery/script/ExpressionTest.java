package io.battery.script;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import io.battery.VariableSource;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class ExpressionTest {
    public static BatteryScript mockInstance() {
        return new BatteryScript();
    }

    private static final String CONSTANTS = " a=1.0; b=2.0; c=3.0; d=10.0; e=20.0;";

    public static Stream<Arguments> literals = Stream.of(
            Arguments.of(10, "r=10;"),
            Arguments.of(10.0, "r=10d;"),
            Arguments.of(3.14, "r=3.14;"),
            Arguments.of(3.14, "r=3.14d;"),
            Arguments.of(new BigDecimal("11"), "r=11bd;"),
            Arguments.of(new BigDecimal("3.14"), "r=3.14bd;"),
            Arguments.of("abc", "r=\"abc\";"),
            Arguments.of(LocalDate.of(2020, 2, 12), "r={d'2020-02-12'} ;"),
            Arguments.of(LocalTime.of(11, 12, 13), "r={t'11:12:13'} ;"),
            Arguments.of(LocalDateTime.of(2020, 2, 12, 11, 12, 13), "r={dt'2020-02-12 11:12:13'} ;"),
            Arguments.of(List.of(1, 2, 3), "r=L[1,2,3]"),
            Arguments.of(Set.of(1, 2, 3), "r=S[1,2,3]"),
            Arguments.of(Map.of(1, 2, 3, 4), "r=M[1,2,3,4]")
    );

    @ParameterizedTest
    @VariableSource("literals")
    public void givenLiterals_expectCorrectType(Object expected, String expression) {
        Map<String, Object> vars = mockInstance().execute(expression);
        assertThat(vars).isNotEmpty();
        assertThat(vars).hasEntrySatisfying("r", o -> {
//             assertThat(o).isInstanceOf(expected.getClass());
            assertThat(o).isEqualTo(expected);
        });
    }

    public static Stream<Arguments> arithmetics = Stream.of(
            Arguments.of(2, "r=1+1;"),
            Arguments.of(-1, "r=1-2;"),
            Arguments.of(4, "r=2*2;"),
            Arguments.of(2, "r=4/2;"),
            Arguments.of(4 % 2, "r=4 % 2;"),
            Arguments.of((int) Math.pow(2, 3.1), "r=2^3.1;"),
            Arguments.of(2 + 2, "r=2*1+ 2;"),
            Arguments.of(2 * (1 + 2), "r=2*(1+2);"),
            Arguments.of(1 + (2 * 2), "r=1+ 2*2;"),
            Arguments.of((1 + 2) * 2, "r=(1+ 2)*2;"),
            Arguments.of((2 * (1 + 2)) * 2, "r=(2*(1+ 2))*2;"),
            Arguments.of((2 * -(1 + 2)) * 2, "r=(2*-(1+ 2))*2;"),
            Arguments.of((2) * 2, "r=(2*-(1+ -2))*2;"),
            Arguments.of(1.5, "r=1.5;"),
            Arguments.of(.5, "r=0.5;"),
            Arguments.of(1.05 + 0.45, "r=1.05 + 0.45;"),
            Arguments.of(2 % 2, "r=2 mod 2;"),
            Arguments.of(2 % 4, "r=2 % 4;"),
            Arguments.of(2 % 4 - 2, "r=2 % 4-2;"),
            Arguments.of((2 % 4) - 2, "r=(2 % 4)-2;")
    );

    @ParameterizedTest
    @VariableSource("arithmetics")
    public void givenArithmeticExpressions_expectSingleResult(Number expected, String expression) {
        Map<String, Object> vars = mockInstance().execute(expression);
        assertThat(vars).isNotEmpty();
    }

    public static Stream<Arguments> variableArithmetics = Stream.of(
            Arguments.of((-(2.0 + 3.0)) * 10.0, "r=(1.0 * -(2.0 + 3.0)) * 10.0;"),
            Arguments.of((-(2.0 + 3.0)) * 10.0, "r=(a * -(b + c)) * d;"),
            Arguments.of(10.0, "r=(a * -(b + -c)) * d;"),
            Arguments.of(10.0, "r=(a*-(b+-c))*d;"),
            Arguments.of(50.0, "r=(a*(b+ c))*d;"),
            Arguments.of(10.0, "r=(1.0*-(2.0+ -3.0))*10.0;"),
            Arguments.of(3.0, "r=a+b;"),
            Arguments.of(-1.0, "r=a-b;"),
            Arguments.of(2.0, "r=a*b;"),
            Arguments.of(0.5, "r=a/b;"),
            Arguments.of((double) 1 % 2, "r=a%b;"),
            Arguments.of(Math.pow(1, 2), "r=a^b;"),
            Arguments.of((double) 1 * 2 + 3, "r=a*b+c;"),
            Arguments.of((double) 1 * (2 + 3), "r=a*(b+c);"),
            Arguments.of((double) 1 + (2 * 3), "r=a+b*c;"),
            Arguments.of(((double) 1 + 2) * 3, "r=(a+b)*c;"),
            Arguments.of(10.0, "r=d;"),
            Arguments.of(20.0, "r=e;"),
            Arguments.of(1.0 % 2.0, "r=a mod b;"),
            Arguments.of(1.0 % 2.0 - 3.0, "r=a % b-c;"),
            Arguments.of((1.0 % 2.0) - 3.0, "r=(a % b)-c;")
    );

    @ParameterizedTest
    @VariableSource("variableArithmetics")
    public void givenAlgebraArithmeticExpressions_expectSingleResult(Number expected, String expression) {
        Map<String, Object> vars = mockInstance().execute(CONSTANTS + expression);
        assertThat(vars).isNotEmpty();
    }


    public static Stream<Arguments> illegalExpressions = Stream.of(
            Arguments.of(false, "r=;"),
            Arguments.of(false, "r=132.2b;"),
            Arguments.of(false, "r=);"),
            Arguments.of(false, "r=(1));"),
            Arguments.of(false, "r=1+;"),
            Arguments.of(false, "r=1-;"),
            Arguments.of(false, "r=1/;"),
            Arguments.of(false, "r=1*;"),
            Arguments.of(false, "r=1^;"),
            Arguments.of(false, "r=*1;"),
            Arguments.of(false, "r=/1;"),
            Arguments.of(false, "r=(1;"),
            Arguments.of(false, "r=1);"),
            Arguments.of(false, "r=(1*(1;"),
            Arguments.of(false, "r=false+1;"),
            Arguments.of(false, "r=1+false ;"),
            Arguments.of(false, "r=M[1,2,3]")
    );

    @ParameterizedTest
    @VariableSource("illegalExpressions")
    public void givenIllegalExpressions_expectScriptException(Object expected, String expression) {
        Assertions.assertThrows(BatteryScriptException.class, () -> {
            mockInstance().execute(CONSTANTS + expression);
        }, "Expected [" + expected + "] for [" + expression + "]");
    }
}
