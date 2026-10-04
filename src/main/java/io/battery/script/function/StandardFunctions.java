package io.battery.script.function;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * General-purpose script functions in the {@code std} namespace, such as the current date
 * and time, sizes and random selection. A mixin of default methods registered by
 * {@link io.battery.script.BatteryScript#registerStandardFunctions()}.
 */
public interface StandardFunctions {
    String NAMESPACE = "std";

    double PI = Math.PI;

    double E = Math.E;

    @Description(value = """
            Returns the current date.
            """, volatility = Volatility.Volatile)
    default LocalDate currentDate() {
        return LocalDate.now();
    }

    @Description(value = """
            Returns the current time.
            """, volatility = Volatility.Volatile)
    default LocalTime currentTime() {
        return LocalTime.now();
    }

    @Description(value = """
            Returns the current date and time.
            """, volatility = Volatility.Volatile)
    default LocalDateTime currentDateTime() {
        return LocalDateTime.now();
    }

    @Description(value = """
            Returns the size of a collection, map or array argument, or otherwise the number of arguments.
            """, volatility = Volatility.Immutable)
    default int sizeOf(Object... args) {
        if (args.length == 1) {
            // A single collection argument arrives wrapped in the varargs array
            Object arg = args[0];
            if (arg == null) {
                return 0;
            }
            if (arg instanceof Collection<?> collection) {
                return collection.size();
            }
            if (arg instanceof Map<?, ?> map) {
                return map.size();
            }
            if (arg.getClass().isArray()) {
                return Array.getLength(arg);
            }
        }
        return args.length;
    }

    @Description(value = """
            Select a random item from a list collection.
            """, volatility = Volatility.Volatile)
    default <E> E selectRandom(List<E> collection) {
        return collection.get(ThreadLocalRandom.current().nextInt(collection.size()));
    }
}
