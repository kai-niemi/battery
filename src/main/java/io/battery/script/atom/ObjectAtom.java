package io.battery.script.atom;

/**
 * Fallback atom wrapping any Java object without a more specific atom type, such as maps
 * and futures. Supports equality and string conversion, and serves as a target for field
 * access and method calls.
 */
public class ObjectAtom implements AtomValue {
    private final Object value;

    public ObjectAtom(Object value) {
        this.value = value;
    }

    @Override
    public boolean isEqualTo(AtomValue right) {
        return asObject().equals(right.asObject());
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
        return "ObjectAtom{" +
                "value=" + value +
                '}';
    }
}
