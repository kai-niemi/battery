package io.battery.scenario;

import java.util.Objects;

/**
 * A cancellation flag with a reason, shared by the phases, steps and workers of a
 * scenario launch and checked cooperatively to stop work.
 * <p>
 * Thread-safe for a cancellation by one thread observed by many: both fields are
 * volatile so that cancellation is visible to all threads checking the marker.
 */
public class CancellationMarker {
    private volatile String reason;

    private volatile boolean cancelled;

    public void markCancelled(String reason) {
        Objects.requireNonNull(reason);
        // Write the reason before the flag, so a thread observing the flag also observes the reason
        this.reason = reason;
        this.cancelled = true;
    }

    public String getReason() {
        return reason;
    }

    public boolean check() {
        return cancelled;
    }
}
