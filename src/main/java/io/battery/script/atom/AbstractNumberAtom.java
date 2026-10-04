package io.battery.script.atom;

import java.math.BigDecimal;

/**
 * Base class for numeric atoms, implementing arithmetic and comparison with numeric
 * promotion of numeric operands (see {@link NumericSupport}). A {@code +} with a string
 * operand concatenates, and other non-numeric operands are converted to the type of this
 * atom, such as parsing a string.
 *
 * @param <N> the wrapped number type
 */
abstract class AbstractNumberAtom<N extends Number> implements AtomValue {
    protected final N value;

    protected AbstractNumberAtom(N value) {
        this.value = value;
    }

    /**
     * Converts a non-numeric operand to the type of this atom.
     */
    protected abstract Number convert(AtomValue operand);

    private Number operand(AtomValue right) {
        Number number = NumericSupport.numeric(right);
        return number != null ? number : convert(right);
    }

    @Override
    public AtomValue plus(AtomValue addend) {
        if (addend instanceof StringAtom) {
            return new StringAtom(asString() + addend.asString());
        }
        return NumericSupport.plus(value, operand(addend));
    }

    @Override
    public AtomValue minus(AtomValue subtrahend) {
        return NumericSupport.minus(value, operand(subtrahend));
    }

    @Override
    public AtomValue multiply(AtomValue multiplier) {
        return NumericSupport.multiply(value, operand(multiplier));
    }

    @Override
    public AtomValue divide(AtomValue divisor) {
        return NumericSupport.divide(value, operand(divisor));
    }

    @Override
    public AtomValue remainder(AtomValue divisor) {
        return NumericSupport.remainder(value, operand(divisor));
    }

    @Override
    public AtomValue pow(AtomValue exponent) {
        return NumericSupport.pow(value, operand(exponent));
    }

    @Override
    public boolean isEqualTo(AtomValue right) {
        return NumericSupport.isEqual(value, operand(right));
    }

    @Override
    public boolean isGreaterThan(AtomValue right) {
        Integer c = NumericSupport.compare(value, operand(right));
        return c != null && c > 0;
    }

    @Override
    public boolean isGreaterThanOrEqualTo(AtomValue right) {
        Integer c = NumericSupport.compare(value, operand(right));
        return c != null && c >= 0;
    }

    @Override
    public boolean isLessThan(AtomValue right) {
        Integer c = NumericSupport.compare(value, operand(right));
        return c != null && c < 0;
    }

    @Override
    public boolean isLessThanOrEqualTo(AtomValue right) {
        Integer c = NumericSupport.compare(value, operand(right));
        return c != null && c <= 0;
    }

    @Override
    public Integer asInteger() {
        return value.intValue();
    }

    @Override
    public Long asLong() {
        return value.longValue();
    }

    @Override
    public Double asDouble() {
        return value.doubleValue();
    }

    @Override
    public BigDecimal asBigDecimal() {
        return NumericSupport.toBigDecimal(value);
    }

    @Override
    public String asString() {
        return String.valueOf(value);
    }

    @Override
    public Object asObject() {
        return value;
    }

    @Override
    public String toString() {
        return "%s{value=%s}".formatted(getClass().getSimpleName(), value);
    }
}
