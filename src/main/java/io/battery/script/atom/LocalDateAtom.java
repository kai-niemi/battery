package io.battery.script.atom;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Atom wrapping a {@code LocalDate}, the type of date literals. Supports chronological ordering
 * against another date value, and equality, which is false for values of other types.
 * Converts to a string in ISO-8601 format.
 */
public class LocalDateAtom implements AtomValue {
    private final LocalDate value;

    public LocalDateAtom(LocalDate value) {
        this.value = value;
    }

    private static LocalDate other(AtomValue right) {
        // Unsupported rather than an assertion, so that the visitor reports a script error
        if (right.asObject() instanceof LocalDate other) {
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
        return "LocalDateAtom{" +
               "value=" + value +
               '}';
    }
}
