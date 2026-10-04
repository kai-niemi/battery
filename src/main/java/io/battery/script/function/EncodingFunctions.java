package io.battery.script.function;

import java.util.Base64;

import io.battery.util.DigestUtils;
import io.battery.util.HexUtils;

/**
 * Encoding functions in the {@code encoding} namespace for Base64, hex and SHA-256.
 * A mixin of default methods registered with the standard functions.
 */
public interface EncodingFunctions {
    String NAMESPACE = "encoding";

    @Description(value = """
            Encodes a byte array to a Base64 string.
            """, volatility = Volatility.Immutable)
    default String toBase64(byte[] arr) {
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(arr);
    }

    @Description(value = """
            Decodes a Base64 string to a byte array.
            """, volatility = Volatility.Immutable)
    default byte[] fromBase64(String text) {
        Base64.Decoder decoder = Base64.getDecoder();
        return decoder.decode(text);
    }

    @Description(value = """
            Returns a secure SHA-256 hash for an object.
            """, volatility = Volatility.Immutable)
    default String toSecureHash(Object object) {
        return DigestUtils.toSecureHash(object);
    }

    @Description(value = """
            Encodes a byte array to hex.
            """, volatility = Volatility.Immutable)
    default String toHex(byte[] bytes) {
        return HexUtils.toHex(bytes);
    }

    @Description(value = """
            Decodes a hex encoded string.
            """, volatility = Volatility.Immutable)
    default String fromHex(String hex) {
        return HexUtils.fromHex(hex.toCharArray());
    }
}
