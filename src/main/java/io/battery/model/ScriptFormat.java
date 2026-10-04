package io.battery.model;

import java.nio.file.Path;
import java.util.Objects;

/**
 * The format of a step's script file, resolved from its file extension: {@code .b}
 * for Battery scripts and {@code .sql} for SQL.
 */
public enum ScriptFormat {
    BATTERY {
        @Override
        public boolean accepts(Path location) {
            return !Objects.isNull(location)
                   && (location.toString().endsWith(".b"));
        }
    },
    SQL {
        @Override
        public boolean accepts(Path location) {
            return !Objects.isNull(location)
                   && location.toString().endsWith(".sql");
        }
    },
    UNKNOWN {
        @Override
        public boolean accepts(Path location) {
            return false;
        }
    };

    public abstract boolean accepts(Path location);
}
