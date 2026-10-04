package io.battery.script;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@Tag("unit-test")
public class ForkJoinTest {
    public static BatteryScript mockInstance() {
        return new BatteryScript();
    }

    @Test
    public void givenSingleForkJoinStatement_expectSuccess() {
        Map<String, Object> vars = mockInstance().execute("""
                f1 = fork {
                  a = 10;
                  if (a == 10) {
                     return a+1;;
                  }
                  return a;
                };
                
                join [f1];
                
                rv=f1.get();
                """);

        Assertions.assertThat(vars).containsKey("rv");
        Assertions.assertThat(vars.get("rv")).isEqualTo(11);
    }

    @Test
    public void givenErrorInForkJoinStatement_expectPropagation() {
        assertThatExceptionOfType(BatteryScriptException.class).isThrownBy(() -> {
            Map<String, Object> vars = mockInstance().execute("""
                    f1 = fork {
                      a = 10;
                      if (b == 10) { // no such var
                         return a+1;;
                      }
                      return a;
                    };
                    
                    join [f1];
                    
                    rv=f1.get();
                    """);
        }).withMessageStartingWith("Cannot resolve symbol 'b' at position 3:6");
    }

    @Test
    public void givenForkJoinStatement_expectSuccess() {
        Map<String, Object> vars = mockInstance().execute("""
                f1 = fork {
                  a = 10;
                  return a;
                };
                
                f2 = fork {
                  b = 20;
                  return b;
                };
                
                join [f1,f2];
                
                rv = f1.get() + f2.get();
                """);

        // Variables assigned in forks are local to the fork
        Assertions.assertThat(vars).doesNotContainKey("a");
        Assertions.assertThat(vars).doesNotContainKey("b");
        Assertions.assertThat(vars.get("rv")).isEqualTo(10 + 20);
    }
}
