package io.battery.script.atom;

/**
 * Atom wrapping a {@code Long}, typically returned from Java interop. Supports arithmetic and
 * ordering with numeric promotion of the operands, and concatenation with strings.
 */
public class LongAtom extends AbstractNumberAtom<Long> {
    public LongAtom(Long value) {
        super(value);
    }

    @Override
    protected Number convert(AtomValue right) {
        return right.asLong();
    }

    @Override
    public AtomValue negate() {
        return new LongAtom(Math.negateExact(value));
    }
}
