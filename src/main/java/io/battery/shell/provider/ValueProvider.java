package io.battery.shell.provider;

@FunctionalInterface
public interface ValueProvider<T> {
    Object getValue(T object, int column);
}
