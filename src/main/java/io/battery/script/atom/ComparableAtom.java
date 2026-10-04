package io.battery.script.atom;

/**
 * Atom wrapping a Java {@code Comparable} without a more specific atom type, such as
 * {@code Float}, enums or {@code Instant}. Supports equality and ordering via
 * {@code compareTo} against the unwrapped right operand, but no arithmetic.
 */
public class ComparableAtom implements AtomValue {
    private final Comparable<Object> value;

    public ComparableAtom(Comparable<Object> value) {
        this.value = value;
    }

    @Override
    public boolean isEqualTo(AtomValue right) {
        return value.equals(right.asObject());
    }

    @Override
    public boolean isGreaterThan(AtomValue right) {
        return value.compareTo(right.asObject()) > 0;
    }

    @Override
    public boolean isGreaterThanOrEqualTo(AtomValue right) {
        return value.compareTo(right.asObject()) >= 0;
    }

    @Override
    public boolean isLessThan(AtomValue right) {
        return value.compareTo(right.asObject()) < 0;
    }

    @Override
    public boolean isLessThanOrEqualTo(AtomValue right) {
        return value.compareTo(right.asObject()) <= 0;
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
        return "ComparableAtom{" +
                "value=" + value +
                '}';
    }
}
