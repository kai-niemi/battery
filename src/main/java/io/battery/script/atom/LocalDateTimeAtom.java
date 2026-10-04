package io.battery.script.atom;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Atom wrapping a {@code LocalDateTime}, the type of datetime literals. Supports chronological ordering
 * against another datetime value, and equality, which is false for values of other types.
 * Converts to a string in ISO-8601 format.
 */
public class LocalDateTimeAtom implements AtomValue {
    private final LocalDateTime value;

    public LocalDateTimeAtom(LocalDateTime value) {
        this.value = value;
    }

    private static LocalDateTime other(AtomValue right) {
        // Unsupported rather than an assertion, so that the visitor reports a script error
        if (right.asObject() instanceof LocalDateTime other) {
            return other;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEqualTo(AtomValue right) {
        return Objects.equals(value, right.asObject());
    }

    @Override
    public boolean isGreaterThan(AtomValue right) {
        return value.isAfter(other(right));
    }

    @Override
    public boolean isGreaterThanOrEqualTo(AtomValue right) {
        return !value.isBefore(other(right));
    }

    @Override
    public boolean isLessThan(AtomValue right) {
        return value.isBefore(other(right));
    }

    @Override
    public boolean isLessThanOrEqualTo(AtomValue right) {
        return !value.isAfter(other(right));
    }

    @Override
    public String asString() {
        return value.toString();
    }

    @Override
    public Object asObject() {
        return value;
    }

    @Override
    public String toString() {
        return "LocalDateTimeAtom{" +
               "value=" + value +
               '}';
    }
}
