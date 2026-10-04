package io.battery.script.function.support;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import io.battery.script.function.DefaultJdbcFunctions;

@Tag("unit-test")
public class JdbcArgumentValidationTest {
    // Never connected, since argument types are validated before executing any SQL
    private final DefaultJdbcFunctions functions
            = new DefaultJdbcFunctions(new DriverManagerDataSource("jdbc:unused"));

    @Test
    public void givenUnknownTypeName_expectClearError() {
        Assertions.assertThatThrownBy(() -> functions.update("update t set c = ?",
                        new Object[] {1}, new String[] {"NUMBERISH"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Unknown SQL type 'NUMBERISH'");
    }

    @Test
    public void givenMismatchedTypeCount_expectClearError() {
        Assertions.assertThatThrownBy(() -> functions.update("update t set c = ?, d = ?",
                        new Object[] {1, 2}, new String[] {"INTEGER"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected 2 argument types but got 1");
    }
}
