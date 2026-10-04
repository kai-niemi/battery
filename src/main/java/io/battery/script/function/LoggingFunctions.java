package io.battery.script.function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging functions in the {@code log} namespace, writing to SLF4J at debug, info, warn
 * and error level. A mixin of default methods.
 */
public interface LoggingFunctions {
    String NAMESPACE = "log";

    Logger logger = LoggerFactory.getLogger(LoggingFunctions.class);

    @Description(value = """
            Logs a message at debug level.
            """, volatility = Volatility.Volatile)
    default void debug(String message) {
        logger.debug(message);
    }

    @Description(value = """
            Logs a message at debug level with {} placeholders replaced by the arguments.
            """, volatility = Volatility.Volatile)
    default void debug(String message, Object... args) {
        logger.debug(message, args);
    }

    @Description(value = """
            Logs a message at info level.
            """, volatility = Volatility.Volatile)
    default void info(String message) {
        logger.info(message);
    }

    @Description(value = """
            Logs a message at info level with {} placeholders replaced by the arguments.
            """, volatility = Volatility.Volatile)
    default void info(String message, Object... args) {
        logger.info(message, args);
    }

    @Description(value = """
            Logs a message at warn level.
            """, volatility = Volatility.Volatile)
    default void warn(String message) {
        logger.warn(message);
    }

    @Description(value = """
            Logs a message at warn level with {} placeholders replaced by the arguments.
            """, volatility = Volatility.Volatile)
    default void warn(String message, Object... args) {
        logger.warn(message, args);
    }

    @Description(value = """
            Logs a message at error level.
            """, volatility = Volatility.Volatile)
    default void error(String message) {
        logger.error(message);
    }

    @Description(value = """
            Logs a message at error level with {} placeholders replaced by the arguments.
            """, volatility = Volatility.Volatile)
    default void error(String message, Object... args) {
        logger.error(message, args);
    }
}
