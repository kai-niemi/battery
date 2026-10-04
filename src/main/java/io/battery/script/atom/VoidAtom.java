package io.battery.script.atom;

/**
 * Atom returned by statements that produce no value, such as loops and joins.
 * Distinct from {@link NullAtom}; unwraps to null and supports no operations.
 */
public class VoidAtom implements AtomValue{
    @Override
    public String toString() {
        return "VoidAtom{}";
    }

    @Override
    public Object asObject() {
        return null;
    }
}
