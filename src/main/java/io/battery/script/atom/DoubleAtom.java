package io.battery.script.atom;

/**
 * Atom wrapping a {@code Double}. Supports IEEE 754 arithmetic and ordering with numeric
 * promotion of the operands, and concatenation with strings. Converts to BigDecimal using
 * its decimal string representation.
 */
public class DoubleAtom extends AbstractNumberAtom<Double> {
    public DoubleAtom(Double value) {
        super(value);
    }

    @Override
    protected Number convert(AtomValue right) {
        return right.asDouble();
    }

    @Override
    public AtomValue negate() {
        return new DoubleAtom(-value);
    }
}
