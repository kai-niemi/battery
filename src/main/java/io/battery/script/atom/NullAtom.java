package io.battery.script.atom;

/**
 * Atom representing the {@code null} literal and null results from Java interop.
 * Unwraps to null and supports no operations.
 */
public class NullAtom implements AtomValue {
    @Override
    public String toString() {
        return "NullAtom{}";
    }

    @Override
    public Object asObject() {
        return null;
    }
}
