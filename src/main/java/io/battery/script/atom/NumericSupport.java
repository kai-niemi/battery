package io.battery.script.atom;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Numeric promotion and arithmetic for numeric atoms, following Java binary numeric
 * promotion extended with {@code BigDecimal}: operands are promoted to the widest type of
 * Integer, Long, Double and BigDecimal before the operation. Integer and Long arithmetic is
 * exact, failing with an {@link ArithmeticException} on overflow or division by zero, and
 * integral powers with a negative exponent produce a Double. BigDecimal division, remainder
 * and powers round to 34 significant digits ({@link MathContext#DECIMAL128}), and BigDecimal
 * equality is numeric, ignoring scale.
 */
final class NumericSupport {
    static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;

    private enum Kind {
        INTEGER, LONG, DOUBLE, BIG_DECIMAL
    }

    private NumericSupport() {
    }

    /**
     * @return the numeric value of the atom, or null if not numeric
     */
    static Number numeric(AtomValue atom) {
        if (atom instanceof AbstractNumberAtom<?> numberAtom) {
            return numberAtom.value;
        }
        if (atom instanceof NamespaceAtom) {
            Object value = atom.asObject();
            if (value instanceof Integer || value instanceof Long
                || value instanceof Double || value instanceof BigDecimal) {
                return (Number) value;
            }
        }
        return null;
    }

    private static Kind kind(Number n) {
        return switch (n) {
            case Integer i -> Kind.INTEGER;
            case Long l -> Kind.LONG;
            case Double d -> Kind.DOUBLE;
            case BigDecimal bd -> Kind.BIG_DECIMAL;
            default -> throw new UnsupportedOperationException("Unsupported numeric type: " + n.getClass());
        };
    }

    private static Kind widest(Number left, Number right) {
        Kind l = kind(left);
        Kind r = kind(right);
        return l.ordinal() >= r.ordinal() ? l : r;
    }

    static BigDecimal toBigDecimal(Number n) {
        return switch (n) {
            case BigDecimal bd -> bd;
            case Double d -> {
                if (d.isNaN() || d.isInfinite()) {
                    throw new UnsupportedOperationException("Non-finite value: " + d);
                }
                // Decimal string representation rather than the exact binary expansion
                yield BigDecimal.valueOf(d);
            }
            default -> BigDecimal.valueOf(n.longValue());
        };
    }

    static AtomValue plus(Number left, Number right) {
        return switch (widest(left, right)) {
            case INTEGER -> new IntegerAtom(Math.addExact(left.intValue(), right.intValue()));
            case LONG -> new LongAtom(Math.addExact(left.longValue(), right.longValue()));
            case DOUBLE -> new DoubleAtom(left.doubleValue() + right.doubleValue());
            case BIG_DECIMAL -> new BigDecimalAtom(toBigDecimal(left).add(toBigDecimal(right)));
        };
    }

    static AtomValue minus(Number left, Number right) {
        return switch (widest(left, right)) {
            case INTEGER -> new IntegerAtom(Math.subtractExact(left.intValue(), right.intValue()));
            case LONG -> new LongAtom(Math.subtractExact(left.longValue(), right.longValue()));
            case DOUBLE -> new DoubleAtom(left.doubleValue() - right.doubleValue());
            case BIG_DECIMAL -> new BigDecimalAtom(toBigDecimal(left).subtract(toBigDecimal(right)));
        };
    }

    static AtomValue multiply(Number left, Number right) {
        return switch (widest(left, right)) {
            case INTEGER -> new IntegerAtom(Math.multiplyExact(left.intValue(), right.intValue()));
            case LONG -> new LongAtom(Math.multiplyExact(left.longValue(), right.longValue()));
            case DOUBLE -> new DoubleAtom(left.doubleValue() * right.doubleValue());
            case BIG_DECIMAL -> new BigDecimalAtom(toBigDecimal(left).multiply(toBigDecimal(right)));
        };
    }

    static AtomValue divide(Number left, Number right) {
        return switch (widest(left, right)) {
            case INTEGER -> new IntegerAtom(left.intValue() / right.intValue());
            case LONG -> new LongAtom(left.longValue() / right.longValue());
            case DOUBLE -> new DoubleAtom(left.doubleValue() / right.doubleValue());
            case BIG_DECIMAL -> new BigDecimalAtom(toBigDecimal(left).divide(toBigDecimal(right), MATH_CONTEXT));
        };
    }

    static AtomValue remainder(Number left, Number right) {
        return switch (widest(left, right)) {
            case INTEGER -> new IntegerAtom(left.intValue() % right.intValue());
            case LONG -> new LongAtom(left.longValue() % right.longValue());
            case DOUBLE -> new DoubleAtom(left.doubleValue() % right.doubleValue());
            case BIG_DECIMAL -> new BigDecimalAtom(toBigDecimal(left).remainder(toBigDecimal(right), MATH_CONTEXT));
        };
    }

    static AtomValue pow(Number base, Number exponent) {
        return switch (widest(base, exponent)) {
            case INTEGER -> exponent.intValue() < 0
                    ? new DoubleAtom(Math.pow(base.doubleValue(), exponent.doubleValue()))
                    : new IntegerAtom(Math.toIntExact(powExact(base.longValue(), exponent.intValue(), true)));
            case LONG -> exponent.longValue() < 0
                    ? new DoubleAtom(Math.pow(base.doubleValue(), exponent.doubleValue()))
                    : new LongAtom(powExact(base.longValue(), Math.toIntExact(exponent.longValue()), false));
            case DOUBLE -> new DoubleAtom(Math.pow(base.doubleValue(), exponent.doubleValue()));
            case BIG_DECIMAL -> {
                BigDecimal e = toBigDecimal(exponent);
                if (e.stripTrailingZeros().scale() > 0) {
                    throw new UnsupportedOperationException("BigDecimal exponent must be integral: " + e);
                }
                yield new BigDecimalAtom(toBigDecimal(base).pow(e.intValueExact(), MATH_CONTEXT));
            }
        };
    }

    /**
     * Exact integral power by repeated squaring, throwing ArithmeticException on overflow.
     */
    private static long powExact(long base, int exponent, boolean intRange) {
        long result = 1;
        long b = base;
        int e = exponent;
        while (e > 0) {
            if ((e & 1) == 1) {
                result = Math.multiplyExact(result, b);
                if (intRange) {
                    Math.toIntExact(result);
                }
            }
            e >>= 1;
            if (e > 0) {
                b = Math.multiplyExact(b, b);
            }
        }
        return result;
    }

    static boolean isEqual(Number left, Number right) {
        return switch (widest(left, right)) {
            case INTEGER, LONG -> left.longValue() == right.longValue();
            case DOUBLE -> left.doubleValue() == right.doubleValue();
            case BIG_DECIMAL -> toBigDecimal(left).compareTo(toBigDecimal(right)) == 0;
        };
    }

    /**
     * @return the comparison result, or null if unordered (NaN operand)
     */
    static Integer compare(Number left, Number right) {
        return switch (widest(left, right)) {
            case INTEGER, LONG -> Long.compare(left.longValue(), right.longValue());
            case DOUBLE -> {
                double l = left.doubleValue();
                double r = right.doubleValue();
                yield Double.isNaN(l) || Double.isNaN(r) ? null : (l < r ? -1 : (l > r ? 1 : 0));
            }
            case BIG_DECIMAL -> toBigDecimal(left).compareTo(toBigDecimal(right));
        };
    }
}
