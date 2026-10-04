package io.battery.script.atom;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.ClassUtils;

import io.battery.script.support.ReflectionSupport;

/**
 * Atom for a dotted name not bound to a variable, such as {@code java.lang.Math.PI},
 * resolved lazily as a class and static field using the application class loader, so any
 * class on the classpath can be used.
 * Operations delegate to the atom of the resolved field value.
 */
public class NamespaceAtom implements AtomValue {
    private final List<String> path = new ArrayList<>();

    public NamespaceAtom(NamespaceAtom parent, String value) {
        this.path.addAll(parent.path);
        this.path.add(value);
    }

    public NamespaceAtom(String value) {
        this.path.add(value);
    }

    @Override
    public BigDecimal asBigDecimal() {
        AtomValue o = AtomValue.of(asObject());
        return o.asBigDecimal();
    }

    @Override
    public Integer asInteger() {
        AtomValue o = AtomValue.of(asObject());
        return o.asInteger();
    }

    @Override
    public Boolean asBoolean() {
        AtomValue o = AtomValue.of(asObject());
        return o.asBoolean();
    }

    @Override
    public AtomValue divide(AtomValue divisor) {
        AtomValue o = AtomValue.of(asObject());
        return o.divide(divisor);
    }

    @Override
    public boolean in(AtomValue right) {
        AtomValue o = AtomValue.of(asObject());
        return o.in(right);
    }

    @Override
    public boolean isEqualTo(AtomValue right) {
        AtomValue o = AtomValue.of(asObject());
        return o.isEqualTo(right);
    }

    @Override
    public boolean isGreaterThan(AtomValue right) {
        AtomValue o = AtomValue.of(asObject());
        return o.isGreaterThan(right);
    }

    @Override
    public boolean isGreaterThanOrEqualTo(AtomValue right) {
        AtomValue o = AtomValue.of(asObject());
        return o.isGreaterThanOrEqualTo(right);
    }

    @Override
    public boolean isLessThan(AtomValue right) {
        AtomValue o = AtomValue.of(asObject());
        return o.isLessThan(right);
    }

    @Override
    public boolean isLessThanOrEqualTo(AtomValue right) {
        AtomValue o = AtomValue.of(asObject());
        return o.isLessThanOrEqualTo(right);
    }

    @Override
    public AtomValue minus(AtomValue subtrahend) {
        AtomValue o = AtomValue.of(asObject());
        return o.minus(subtrahend);
    }

    @Override
    public AtomValue multiply(AtomValue multiplier) {
        AtomValue o = AtomValue.of(asObject());
        return o.multiply(multiplier);
    }

    @Override
    public AtomValue negate() {
        AtomValue o = AtomValue.of(asObject());
        return o.negate();
    }

    @Override
    public AtomValue plus(AtomValue addend) {
        AtomValue o = AtomValue.of(asObject());
        return o.plus(addend);
    }

    @Override
    public AtomValue pow(AtomValue addend) {
        AtomValue o = AtomValue.of(asObject());
        return o.pow(addend);
    }

    @Override
    public AtomValue remainder(AtomValue divisor) {
        AtomValue o = AtomValue.of(asObject());
        return o.remainder(divisor);
    }

    @Override
    public String asString() {
        return String.join(".", path);
    }

    @Override
    public Object asObject() {
        Class<?> clazz = asClass();
        return ReflectionSupport.accessField(null, clazz, getLastElement());
    }

    @Override
    public Class<?> asClass() {
        try {
            List<String> adjustedPath = new ArrayList<>(path);
            adjustedPath.removeLast();
            // Application class loader, so that scripts can use any class on the classpath
            return ClassUtils.forName(String.join(".", adjustedPath),
                    ClassUtils.getDefaultClassLoader());
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Cannot resolve symbol '"
                                                    + asString() + "'");
        }
    }

    public String getLastElement() {
        return new ArrayList<>(path).removeLast();
    }
}
