package io.battery.script.atom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Atom wrapping a {@code java.util.Collection}, such as list and set literals. {@code +}
 * returns a new collection of the same kind (list or set) with the right operand added as
 * a single element, leaving the wrapped collection unchanged, and {@code in} tests membership.
 * Null elements are supported.
 */
public class CollectionAtom implements AtomValue {
    private final Collection<Object> value;

    public CollectionAtom(Collection<Object> value) {
        this.value = value;
    }

    @Override
    public AtomValue plus(AtomValue addend) {
        // Copy rather than mutate, since literals may be immutable and values may be aliased
        Collection<Object> copy = value instanceof Set
                ? new LinkedHashSet<>(value)
                : new ArrayList<>(value);
        copy.add(addend.asObject());
        return new CollectionAtom(copy);
    }

    @Override
    public boolean in(AtomValue right) {
        Object element = right.asObject();
        return value.stream().anyMatch(e -> Objects.equals(e, element));
    }

    @Override
    public Collection<Object> asIterable() {
        // Copy that allows null elements, unlike List.copyOf
        return Collections.unmodifiableList(new ArrayList<>(value));
    }

    @Override
    public Collection<Object> asObject() {
        return value;
    }

    @Override
    public String toString() {
        return "CollectionAtom{" +
               "value=" + value +
               '}';
    }
}
