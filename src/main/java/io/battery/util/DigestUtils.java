package io.battery.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Computes a SHA-256 fingerprint of an object from its JSON form, with map entries sorted by key
 * so that equal objects hash the same. Used to check that agents run the same model.
 */
public abstract class DigestUtils {
    private static final JsonMapper mapper = JsonMapper.builder()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS) // determinism
            .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    public static final String ALGORITHM = "SHA-256";

    public static <T> String toSecureHash(T object) {
        try {
            String normalizedJson = mapper.writeValueAsString(object);
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(normalizedJson.getBytes(StandardCharsets.UTF_8));
            return HexUtils.toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private DigestUtils() {
    }
}
