package io.battery.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class ByteFormatTest {
    @Test
    public void testByteCountToDisplaySize() {
        assertThat(ByteFormat.byteCountToDisplaySize(0)).isEqualTo("0 bytes");
        assertThat(ByteFormat.byteCountToDisplaySize(500)).isEqualTo("500 bytes");
        assertThat(ByteFormat.byteCountToDisplaySize(999)).isEqualTo("999 bytes");

        assertThat(ByteFormat.byteCountToDisplaySize(1000)).isEqualTo("1 KB");
        assertThat(ByteFormat.byteCountToDisplaySize(1500)).isEqualTo("2 KB");
        assertThat(ByteFormat.byteCountToDisplaySize(10000)).isEqualTo("10 KB");

        assertThat(ByteFormat.byteCountToDisplaySize(1000000)).isEqualTo("1 MB");
        assertThat(ByteFormat.byteCountToDisplaySize(1500000)).isEqualTo("1.5 MB");
        assertThat(ByteFormat.byteCountToDisplaySize(25000000)).isEqualTo("25 MB");

        assertThat(ByteFormat.byteCountToDisplaySize(1000000000)).isEqualTo("1 GB");
        assertThat(ByteFormat.byteCountToDisplaySize(2500000000L)).isEqualTo("2.5 GB");
    }
}
