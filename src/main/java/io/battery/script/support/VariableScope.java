package io.battery.script.support;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import io.battery.script.Constants;
import io.battery.script.atom.AtomValue;

/**
 * Chain of lexical variable scopes used by the visitor, where a child scope reads through to
 * its parent. Assigning a variable updates the nearest scope that already defines it, or
 * otherwise defines it in the assigning scope, so variables are local to the block that first
 * assigns them. A {@link #snapshot()} creates an isolated root scope, as used for forks.
 * A scope can be sealed against assignment, as done for external variables.
 *
 * @param <T> the variable value type
 */
public final class VariableScope<T extends AtomValue> {
    public static boolean isLocalVariable(String name) {
        return name.startsWith("_");
    }

    private final VariableScope<T> parent;

    private final Map<String, T> variables = new ConcurrentHashMap<>();

    private volatile boolean sealed;

    public VariableScope() {
        this.parent = null;
    }

    public VariableScope(VariableScope<T> parent) {
        this.parent = parent;
        this.sealed = parent.sealed;
    }

    public boolean isEmpty() {
        return variables.isEmpty();
    }

    public void seal() {
        this.sealed = true;
    }

    public void assign(String name, T value) {
        Assert.state(!Constants.RESERVED_KEYWORDS.contains(name), "Reserved keyword: " + name);
        assignOrReplace(name, value);
    }

    /**
     * Assigns a variable in the nearest scope that defines it, or otherwise defines it in this scope.
     */
    public void assignOrReplace(String name, T value) {
        Assert.isTrue(!sealed, "Scope has been sealed");
        VariableScope<T> scope = this;
        while (scope != null && !scope.variables.containsKey(name)) {
            scope = scope.parent;
        }
        (scope != null ? scope : this).variables.put(name, value);
    }

    /**
     * @return the root scope of this scope chain
     */
    public VariableScope<T> root() {
        VariableScope<T> scope = this;
        while (scope.parent != null) {
            scope = scope.parent;
        }
        return scope;
    }

    /**
     * Defines a variable in this scope, shadowing any variable with the same name in parent
     * scopes, such as for loop variables.
     */
    public void define(String name, T value) {
        Assert.isTrue(!sealed, "Scope has been sealed");
        variables.put(name, value);
    }

    /**
     * @return an isolated root scope holding a copy of all variables visible from this scope,
     * so that assignments to it don't affect this scope chain and vice versa
     */
    public VariableScope<T> snapshot() {
        VariableScope<T> snapshot = parent != null ? parent.snapshot() : new VariableScope<>();
        snapshot.variables.putAll(variables);
        snapshot.sealed = sealed;
        return snapshot;
    }

    public boolean contains(String name) {
        if (variables.containsKey(name)) {
            return true;
        }
        if (parent != null) {
            return parent.contains(name);
        }
        return false;
    }

    public T get(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.get(name);
        }
        throw new IllegalArgumentException(name + " is not defined");
    }

    public Map<String, Object> getVariables(Predicate<String> predicate) {
        // Not using Collectors.toMap, which rejects null values such as from null assignments
        Map<String, Object> result = new HashMap<>();
        variables.forEach((name, value) -> {
            if (predicate.test(name)) {
                result.put(name, value.asObject());
            }
        });
        return result;
    }
}
