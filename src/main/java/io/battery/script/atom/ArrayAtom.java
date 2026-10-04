package io.battery.script.atom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Atom wrapping an {@code Object[]}, such as array literals. The only atom supporting index
 * access; {@code +} returns a new array with the elements of the right operand appended,
 * and {@code in} tests membership. Null elements are supported, and an array is only equal
 * to another array with deeply equal elements.
 */
public final class ArrayAtom implements AtomValue {
    private final Object[] value;

    public ArrayAtom(Object[] value) {
        this.value = value;
    }

    @Override
    public AtomValue plus(AtomValue addend) {
        // Arrays.asList rather than List.of, which rejects null elements
        List<Object> copy = new ArrayList<>(Arrays.asList(value));
        addend.asIterable().forEach(copy::add);
        return new ArrayAtom(copy.toArray());
    }

    @Override
    public boolean isEqualTo(AtomValue right) {
        return right.asObject() instanceof Object[] other && Arrays.deepEquals(value, other);
    }

    @Override
    public boolean in(AtomValue right) {
        return Arrays.asList(value).contains(right.asObject());
    }

    @Override
    public Collection<Object> asIterable() {
        return Collections.unmodifiableList(Arrays.asList(value));
    }

    @Override
    public Object[] asObject() {
        return value;
    }

    @Override
    public String toString() {
        return "ArrayAtom{" +
               "value=" + Arrays.toString(value) +
               '}';
    }
}
