package io.battery.util.ratelimit;

/**
 * A blocking (permit based) rate limiter object releasing a bounded number of permits.
 */
public interface RateLimiter extends AutoCloseable {
    /**
     * Attempts to acquire a permit, blocks until available.
     *
     * @return true if a permit was acquired, or false without blocking if the rate limiter
     * is exhausted or cancelled
     * @throws InterruptedException if interrupted
     */
    boolean acquire() throws InterruptedException;

    /**
     * @return the total number of permits released before the rate limiter is exhausted
     */
    int getTotalPermits();

    /**
     * Cancel the rate limiter and voids it.
     */
    void cancel();

    @Override
    void close();
}
