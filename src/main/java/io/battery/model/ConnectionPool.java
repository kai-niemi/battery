package io.battery.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Sizing of the connection pool for scenario launches. A virtual user (VU) waiting for a
 * pooled connection isn't running SQL, so the pool caps database concurrency regardless of
 * how many VUs the phases create.
 * <p>
 * With {@code autoSize} enabled, the pool grows for the duration of a launch to the highest
 * max concurrency of the launched phases, up to {@code maxSize}, and shrinks back afterwards.
 * Since the database also limits connections (100 by default for PostgreSQL), {@code maxSize}
 * should leave room for other clients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionPool {
    private boolean autoSize = true;

    // Largest pool size for a launch, or 0 to never grow beyond the configured pool size
    @PositiveOrZero
    private int maxSize = 64;

    public boolean isAutoSize() {
        return autoSize;
    }

    public void setAutoSize(boolean autoSize) {
        this.autoSize = autoSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }
}
