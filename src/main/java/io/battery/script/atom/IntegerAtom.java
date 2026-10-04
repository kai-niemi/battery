package io.battery.script.atom;

/**
 * Atom wrapping an {@code Integer}, the type of integer literals. Supports arithmetic and
 * ordering with numeric promotion of the operands, so the result has the widest operand type
 * (for example {@code 1 + 2.5} is a double). Integer division truncates and overflow is an
 * error. A {@code +} with a string concatenates.
 */
public class IntegerAtom extends AbstractNumberAtom<Integer> {
    public IntegerAtom(Integer value) {
        super(value);
    }

    @Override
    protected Number convert(AtomValue right) {
        return right.asInteger();
    }

    @Override
    public AtomValue negate() {
        return new IntegerAtom(Math.negateExact(value));
    }
}
