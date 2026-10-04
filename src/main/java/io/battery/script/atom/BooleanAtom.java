package io.battery.script.atom;

/**
 * Atom wrapping a {@code Boolean}, the only value accepted as a condition by if, while
 * and logical operators. Supports equality only.
 */
public class BooleanAtom implements AtomValue {
    private final Boolean value;

    public BooleanAtom(Boolean value) {
        this.value = value;
    }

    @Override
    public boolean isEqualTo(AtomValue right) {
        return asBoolean().equals(right.asBoolean());
    }

    @Override
    public Boolean asBoolean() {
        return value;
    }

    @Override
    public String asString() {
        return String.valueOf(value);
    }

    @Override
    public Boolean asObject() {
        return value;
    }

    @Override
    public String toString() {
        return "BooleanAtom{" +
                "value=" + value +
                '}';
    }
}
