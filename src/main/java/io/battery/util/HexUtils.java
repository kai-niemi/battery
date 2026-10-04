package io.battery.util;

import java.nio.charset.StandardCharsets;

/**
 * Converts between bytes and lowercase hexadecimal strings. Decoding accepts either case.
 */
public abstract class HexUtils {
    private HexUtils() {
    }

    public static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String toHex(byte[] bytes) {
        return new String(toHexChars(bytes));
    }

    public static char[] toHexChars(byte[] bytes) {
        int byteCount = bytes.length;
        char[] result = new char[2 * byteCount];
        int j = 0;

        for (byte b : bytes) {
            result[j++] = HEX_CHARS[(240 & b) >>> 4];
            result[j++] = HEX_CHARS[15 & b];
        }

        return result;
    }

    /**
     * @return the UTF-8 string encoded by the hex characters
     * @throws IllegalArgumentException if the characters aren't valid hex
     */
    public static String fromHex(char[] hexChars) {
        return new String(toBytes(hexChars), StandardCharsets.UTF_8);
    }

    /**
     * @throws IllegalArgumentException if the characters aren't valid hex, or are an odd number
     */
    public static byte[] toBytes(char[] hexChars) {
        if (hexChars.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of hex characters: " + hexChars.length);
        }

        byte[] r = new byte[hexChars.length / 2];

        for (int i = 0; i < r.length; i++) {
            int d1 = digit(hexChars, i * 2);
            int d2 = digit(hexChars, i * 2 + 1);
            r[i] = (byte) ((d1 << 4) + d2);
        }

        return r;
    }

    private static int digit(char[] hexChars, int index) {
        char c = hexChars[index];
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        throw new IllegalArgumentException("Invalid hex character '%c' at index %d".formatted(c, index));
    }
}
