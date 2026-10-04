package io.battery.script.function;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes a script function with a human-readable description and {@link Volatility},
 * shown when listing functions. Informational only; it doesn't affect function resolution
 * or invocation.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Description {
    String value() default "";

    Volatility volatility() default Volatility.Undefined;
}
