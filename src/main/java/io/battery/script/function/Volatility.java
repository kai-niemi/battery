package io.battery.script.function;

/**
 * Informational classification of a script function, shown when listing functions, similar
 * to PostgreSQL function volatility: {@code Immutable} functions always return the same result
 * for the same arguments without side effects, {@code Volatile} functions may return different
 * results between calls (such as random values, the current time, database queries or network
 * lookups) or have side effects, and {@code Undefined} is not declared. It has no effect on
 * evaluation.
 */
public enum Volatility {
    Immutable,
    Volatile,
    Undefined
}
