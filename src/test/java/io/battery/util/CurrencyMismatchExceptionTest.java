package io.battery.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class CurrencyMismatchExceptionTest {
    @Test
    public void testExceptionMessage() {
        CurrencyMismatchException ex = new CurrencyMismatchException("SEK does not match USD");
        assertThat(ex.getMessage()).isEqualTo("SEK does not match USD");
    }
}
