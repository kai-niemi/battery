package io.battery.script.atom;

import java.math.BigDecimal;

/**
 * Atom wrapping a {@code BigDecimal}, the type of {@code bd} suffixed literals, and the widest
 * numeric type in promotion. Division, remainder and powers round to 34 significant digits,
 * powers require an integral exponent, and equality is numeric ignoring scale
 * (for example {@code 1.0bd == 1.00bd}). A {@code +} with a string concatenates.
 */
public final class BigDecimalAtom extends AbstractNumberAtom<BigDecimal> {
    public BigDecimalAtom(BigDecimal value) {
        super(value);
    }

    @Override
    protected Number convert(AtomValue right) {
        return right.asBigDecimal();
    }

    @Override
    public AtomValue negate() {
        return new BigDecimalAtom(value.negate());
    }
}
