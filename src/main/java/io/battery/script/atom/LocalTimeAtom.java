package io.battery.script.atom;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Atom wrapping a {@code LocalTime}, the type of time literals. Supports chronological ordering
 * against another time value, and equality, which is false for values of other types.
 * Converts to a string in ISO-8601 format.
 */
public class LocalTimeAtom implements AtomValue {
    private final LocalTime value;

    public LocalTimeAtom(LocalTime value) {
        this.value = value;
    }

    private static LocalTime other(AtomValue right) {
        // Unsupported rather than an assertion, so that the visitor reports a script error
        if (right.asObject() instanceof LocalTime other) {
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
        return "LocalTimeAtom{" +
               "value=" + value +
               '}';
    }
}
