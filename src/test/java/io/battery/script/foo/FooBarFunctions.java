package io.battery.script.foo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FooBarFunctions {
    public static final String NAMESPACE = "foobar";

    public String[] arrayOfStrings() {
        return new String[] {"a", "b", "c"};
    }

    public List<String> listOfStrings() {
        return List.of("a", "b", "c");
    }

    public Set<String> setOfStrings() {
        return Set.of("a", "b", "c");
    }

    public List<Map<String, Object>> listOfMaps() {
        return List.of(
                Map.of("a", 1, "b", 2, "c", 3),
                Map.of("a", 4, "b", 5, "c", 6),
                Map.of("a", 7, "b", 8, "c", 9)
        );
    }

    public Foo foo() {
        return new Foo();
    }

    public Bar bar() {
        return new Bar();
    }
}
