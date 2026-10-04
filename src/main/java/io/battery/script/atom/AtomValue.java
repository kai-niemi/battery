package io.battery.script.atom;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Objects;

/**
 * Runtime value of a battery script expression, wrapping a Java object. Declares the
 * arithmetic, comparison, membership and conversion operations, which are unsupported
 * by default. Binary operations are dispatched on the left operand. Numeric operands are
 * promoted to the widest numeric type (see {@link AbstractNumberAtom}), while other operands
 * are converted by the left operand as needed. Use {@link #of(Object)} to wrap a Java value
 * in its atom type.
 */
public interface AtomValue {
    AtomValue NULL = new NullAtom();

    AtomValue VOID = new VoidAtom();

    @SuppressWarnings({"rawtypes", "unchecked"})
    static AtomValue of(Object value) {
        if (value instanceof BigDecimal) {
            return new BigDecimalAtom((BigDecimal) value);
        } else if (value instanceof Integer) {
            return new IntegerAtom((int) value);
        } else if (value instanceof Long) {
            return new LongAtom((long) value);
        } else if (value instanceof Double) {
            return new DoubleAtom((double) value);
        } else if (value instanceof Short || value instanceof Byte) {
            // Normalized to the numeric atom types so they take part in numeric promotion
            return new IntegerAtom(((Number) value).intValue());
        } else if (value instanceof Float f) {
            // Decimal value of the float, rather than its binary expansion as a double
            return new DoubleAtom(Double.valueOf(f.toString()));
        } else if (value instanceof BigInteger bi) {
            return new BigDecimalAtom(new BigDecimal(bi));
        } else if (value instanceof Boolean) {
            return new BooleanAtom((Boolean) value);
        } else if (value instanceof String) {
            return new StringAtom((String) value);
        } else if (value instanceof Collection) {
            return new CollectionAtom((Collection) value);
        } else if (value instanceof LocalTime) {
            return new LocalTimeAtom((LocalTime) value);
        } else if (value instanceof LocalDate) {
            return new LocalDateAtom((LocalDate) value);
        } else if (value instanceof LocalDateTime) {
            return new LocalDateTimeAtom((LocalDateTime) value);
        } else if (value instanceof AtomValue) {
            return (AtomValue) value;
        } else if (value instanceof Comparable) {
            return new ComparableAtom((Comparable) value);
        } else if (value instanceof Object[]) {
            return new ArrayAtom((Object[]) value);
        } else if (value == null) {
            return NULL;
        } else {
            return new ObjectAtom(value);
        }
    }

    default AtomValue pow(AtomValue addend) {
        throw new UnsupportedOperationException();
    }

    default AtomValue negate() {
        throw new UnsupportedOperationException();
    }

    default AtomValue plus(AtomValue addend) {
        throw new UnsupportedOperationException();
    }

    default AtomValue minus(AtomValue subtrahend) {
        throw new UnsupportedOperationException();
    }

    default AtomValue multiply(AtomValue multiplier) {
        throw new UnsupportedOperationException();
    }

    default AtomValue divide(AtomValue divisor) {
        throw new UnsupportedOperationException();
    }

    default AtomValue remainder(AtomValue divisor) {
        throw new UnsupportedOperationException();
    }

    default boolean isEqualTo(AtomValue right) {
        return Objects.equals(asObject(), right.asObject());
    }

    default boolean isGreaterThan(AtomValue right) {
        throw new UnsupportedOperationException();
    }

    default boolean isGreaterThanOrEqualTo(AtomValue right) {
        throw new UnsupportedOperationException();
    }

    default boolean isLessThan(AtomValue right) {
        throw new UnsupportedOperationException();
    }

    default boolean in(AtomValue right) {
        throw new UnsupportedOperationException();
    }

    default boolean isLessThanOrEqualTo(AtomValue right) {
        throw new UnsupportedOperationException();
    }

    default Iterable<Object> asIterable() {
        throw new UnsupportedOperationException();
    }

    default BigDecimal asBigDecimal() {
        throw new UnsupportedOperationException();
    }

    default Double asDouble() {
        throw new UnsupportedOperationException();
    }

    default Long asLong() {
        throw new UnsupportedOperationException();
    }

    default Integer asInteger() {
        throw new UnsupportedOperationException();
    }

    default Boolean asBoolean() {
        throw new UnsupportedOperationException();
    }

    default String asString() {
        throw new UnsupportedOperationException("Can't covert [" + getClass().getName() + "] to a string");
    }

    default Object asObject() {
        throw new UnsupportedOperationException();
    }

    default Class<?> asClass() {
        if (asObject() instanceof Class<?>) {
            return (Class<?>) asObject();
        }
        return asObject().getClass();
    }
}
