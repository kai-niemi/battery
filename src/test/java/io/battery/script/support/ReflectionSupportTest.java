package io.battery.script.support;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit-test")
public class ReflectionSupportTest {
    public static class Fixture {
        public String over(int a) {
            return "int";
        }

        public String over(long a) {
            return "long";
        }

        public String over(double a) {
            return "double";
        }

        public String over(String a) {
            return "string";
        }

        public String over(Object a) {
            return "object";
        }

        // Same arity as update(sql, a, b) but different parameter types, like JdbcFunctions
        public String update(String sql, Object[] args, String[] types) {
            return "typed";
        }

        public String update(String sql, Object... args) {
            return "varargs:" + args.length;
        }

        public String one(String s) {
            return "fixed";
        }

        public String one(String s, Object... args) {
            return "varargs";
        }

        public int count(String s, Object... args) {
            return args == null ? -1 : args.length;
        }

        public String mixed(int a) {
            return "instance";
        }

        public static String mixed(long a) {
            return "static";
        }
    }

    private static Object invoke(Object target, String method, Object... args) {
        return ReflectionSupport.invoke(target, Fixture.class, method, Arrays.asList(args));
    }

    @Test
    public void givenOverloads_expectMostSpecificByArgumentType() {
        Fixture f = new Fixture();
        Assertions.assertThat(invoke(f, "over", 1)).isEqualTo("int");
        Assertions.assertThat(invoke(f, "over", 1L)).isEqualTo("long");
        Assertions.assertThat(invoke(f, "over", 2.5)).isEqualTo("double");
        Assertions.assertThat(invoke(f, "over", "x")).isEqualTo("string");
        Assertions.assertThat(invoke(f, "over", List.of())).isEqualTo("object");
        // A subtype (Object) is preferred over primitive widening (int), as in Java
        Assertions.assertThat(invoke(f, "over", (short) 1)).isEqualTo("object");
    }

    @Test
    public void givenSameArityOverloads_expectTypeCheckedChoice() {
        // Previously matched by count only, possibly picking the typed overload and failing
        Assertions.assertThat(invoke(new Fixture(), "update", "sql", 1, 2)).isEqualTo("varargs:2");
        Assertions.assertThat(invoke(new Fixture(), "update", "sql", new Object[] {1}, new String[] {"INTEGER"}))
                .isEqualTo("typed");
    }

    @Test
    public void givenFixedAndVarargsOverloads_expectFixedArityPreferred() {
        Assertions.assertThat(invoke(new Fixture(), "one", "x")).isEqualTo("fixed");
        Assertions.assertThat(invoke(new Fixture(), "one", "x", 1)).isEqualTo("varargs");
    }

    @Test
    public void givenVarargs_expectNullAsSingleElementAndArrayPassedThrough() {
        Fixture f = new Fixture();
        Assertions.assertThat(invoke(f, "count", "x", null)).isEqualTo(1);
        Assertions.assertThat(invoke(f, "count", "x", new Object[] {1, 2})).isEqualTo(2);
        Assertions.assertThat(invoke(f, "count", "x")).isEqualTo(0);
        Assertions.assertThat(invoke(f, "count", "x", 1, 2, 3)).isEqualTo(3);
    }

    @Test
    public void givenPrimitiveWidening_expectInvocation() {
        Object max = ReflectionSupport.invoke(null, Math.class, "max", List.of(1, 2.5));
        Assertions.assertThat(max).isEqualTo(2.5);
    }

    @Test
    public void givenStaticAndInstanceOverloads_expectInvocableMethodOnly() {
        // Instance methods can't be invoked on a class reference, so the static overload applies
        Assertions.assertThat(invoke(null, "mixed", 1)).isEqualTo("static");
        Assertions.assertThat(invoke(new Fixture(), "mixed", 1)).isEqualTo("instance");
    }
}
