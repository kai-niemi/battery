package io.battery.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class HexUtilsTest {
    @Test
    public void whenConvertingBytesToHex_expectHexString() {
        String hex = HexUtils.toHex("The quick brown fox jumps over the lazy dog".getBytes());
        assertThat(hex).isEqualTo(
                "54686520717569636b2062726f776e20666f78206a756d7073206f76657220746865206c617a7920646f67");

        char[] chars = HexUtils.toHexChars("The quick brown fox jumps over the lazy dog".getBytes());

        assertThat(chars).isEqualTo(
                "54686520717569636b2062726f776e20666f78206a756d7073206f76657220746865206c617a7920646f67".toCharArray());

        String text = HexUtils.fromHex(
                "54686520717569636b2062726f776e20666f78206a756d7073206f76657220746865206c617a7920646f67".toCharArray());
        assertThat(text).isEqualTo("The quick brown fox jumps over the lazy dog");

        // Test uppercase hex characters
        String upperText = HexUtils.fromHex("414243444546".toCharArray());
        assertThat(upperText).isEqualTo("ABCDEF");

        byte[] emptyBytes = HexUtils.toBytes(new char[0]);
        assertThat(emptyBytes).isEmpty();
    }

    @Test
    public void whenConvertingInvalidHex_expectException() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> HexUtils.toBytes("abc".toCharArray()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Odd number of hex characters: 3");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> HexUtils.toBytes("4g".toCharArray()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid hex character 'g' at index 1");
    }

    @Test
    public void whenConvertingHexOfUtf8_expectString() {
        assertThat(HexUtils.fromHex(HexUtils.toHexChars("åäö €".getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .isEqualTo("åäö €");
    }
}
