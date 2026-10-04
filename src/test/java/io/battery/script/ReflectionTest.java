package io.battery.script;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.script.support.ReflectionSupport;

@Tag("unit-test")
public class ReflectionTest {
    @Test
    public void givenStringFormat_expectExpansion() {
        List<Object> args = new ArrayList<>();
        args.add(new Object[] {"Alice", 25});

        String formatted = (String) ReflectionSupport.invoke(
                "Hello %s, age %d",
                String.class,
                "formatted",
                args
        );

        Assertions.assertThat(formatted).isNotNull();
        Assertions.assertThat(formatted).isEqualTo("Hello Alice, age 25");
    }
}
