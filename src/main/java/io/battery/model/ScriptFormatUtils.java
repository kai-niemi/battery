package io.battery.model;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

/**
 * Resolves the {@link ScriptFormat} of a script file from its path.
 */
public abstract class ScriptFormatUtils {
    private ScriptFormatUtils() {
    }

    public static Optional<ScriptFormat> resolveFormat(Path path) {
        return Arrays.stream(ScriptFormat.values())
                .filter(scriptFormat -> scriptFormat.accepts(path))
                .findAny();
    }
}
