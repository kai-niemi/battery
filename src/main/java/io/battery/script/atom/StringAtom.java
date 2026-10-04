package io.battery.script.atom;

import java.math.BigDecimal;

/**
 * Atom wrapping a {@code String}. {@code +} concatenates, comparisons are lexicographic
 * against the string form of the right operand, and numeric conversions parse the text.
 */
public final class StringAtom implements AtomValue {
    private final String value;

    public StringAtom(String value) {
        this.value = value;
    }

    @Override
    public BigDecimal asBigDecimal() {
        return new BigDecimal(value);
    }

    @Override
    public Integer asInteger() {
        return asBigDecimal().intValue();
    }

    @Override
    public Long asLong() {
        return asBigDecimal().longValue();
    }

    @Override
    public Double asDouble() {
        return Double.valueOf(value);
    }

    @Override
    public AtomValue plus(AtomValue addend) {
        return new StringAtom(asObject() + addend.asObject());
    }

    private static String stringOf(AtomValue right) {
        // A namespace converts to its dotted path, so compare against its resolved value instead
        return right instanceof NamespaceAtom ? String.valueOf(right.asObject()) : right.asString();
    }

    @Override
    public boolean isEqualTo(AtomValue right) {
        return asObject().equals(stringOf(right));
    }

    @Override
    public boolean isGreaterThan(AtomValue right) {
        return asObject().compareTo(stringOf(right)) > 0;
    }

    @Override
    public boolean isGreaterThanOrEqualTo(AtomValue right) {
        return asObject().compareTo(stringOf(right)) >= 0;
    }

    @Override
    public boolean isLessThan(AtomValue right) {
        return asObject().compareTo(stringOf(right)) < 0;
    }

    @Override
    public boolean isLessThanOrEqualTo(AtomValue right) {
        return asObject().compareTo(stringOf(right)) <= 0;
    }

    @Override
    public String asString() {
        return value;
    }

    @Override
    public String asObject() {
        return value;
    }

    @Override
    public String toString() {
        return "StringAtom{" +
                "value='" + value + '\'' +
                '}';
    }
}
