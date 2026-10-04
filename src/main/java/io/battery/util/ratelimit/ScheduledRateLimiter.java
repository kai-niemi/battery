package io.battery.util.ratelimit;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntToLongFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * A semaphore rate limiter releasing a fixed number of permits one at a time
 * according to a release schedule, after which the rate limiter is exhausted.
 * <p>
 * Each permit is released at an absolute offset from the start time rather than after
 * a relative sleep interval, so that sleep overshoot and rounding don't accumulate.
 * Permits not acquired on schedule are retained, which lets a lagging consumer catch
 * up to the total number of permits by the end of the schedule.
 *
 * @see #fixed(int, Duration)
 * @see #linearRamp(int, int, Duration)
 */
public final class ScheduledRateLimiter implements RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledRateLimiter.class);

    /**
     * Creates a rate limiter releasing a total number of permits evenly over a duration.
     *
     * @param totalPermits the total number of permits to release
     * @param duration     the duration over which the permits are released
     * @return the rate limiter
     */
    public static ScheduledRateLimiter fixed(int totalPermits, Duration duration) {
        Assert.state(totalPermits > 0, "totalPermits must be > 0");
        Assert.state(!duration.isNegative(), "duration must be >= 0");

        logger.trace("Releasing %d permits over %ds (1 permit every %dms)".formatted(
                totalPermits, duration.toSeconds(), duration.dividedBy(totalPermits).toMillis()));

        final long durationNanos = duration.toNanos();
        return new ScheduledRateLimiter(totalPermits,
                permit -> Math.round((double) durationNanos * permit / totalPermits));
    }

    /**
     * Creates a rate limiter releasing permits at a rate increasing linearly from startRate
     * to maxRate permits/sec over a duration. Use the same start and max rate for a constant rate.
     *
     * @param startRate the initial permit release rate (permits/sec)
     * @param maxRate   the final permit release rate (permits/sec)
     * @param duration  the duration over which the rate is increased from startRate to maxRate
     * @return the rate limiter
     */
    public static ScheduledRateLimiter linearRamp(int startRate, int maxRate, Duration duration) {
        Assert.state(startRate >= 0, "startRate must be >= 0");
        Assert.state(maxRate > 0, "maxRate must be > 0");
        Assert.state(startRate <= maxRate, "startRate must be <= maxRate");
        Assert.state(duration.isPositive(), "duration must be > 0");

        // Area under the linear rate curve
        final long durationNanos = duration.toNanos();
        final double durationSeconds = durationNanos / 1e9;
        final int totalPermits = (int) Math.max(1, Math.floor((startRate + maxRate) / 2.0 * durationSeconds));
        // Rate increase in permits/sec^2
        final double acceleration = (maxRate - startRate) / durationSeconds;

        logger.trace("Releasing %d permits ramping from %d to %d permits/sec over %ds".formatted(
                totalPermits, startRate, maxRate, duration.toSeconds()));

        // Permits released by time t are startRate*t + acceleration*t^2/2, solved for t when
        // reaching the given permit. This form of the quadratic root is numerically stable
        // and reduces to permit/startRate for a constant rate.
        return new ScheduledRateLimiter(totalPermits, permit -> {
            double seconds = 2.0 * permit
                             / (startRate + Math.sqrt((double) startRate * startRate + 2.0 * acceleration * permit));
            return Math.min(durationNanos, Math.round(seconds * 1e9));
        });
    }

    private final Semaphore semaphore = new Semaphore(0);

    private final int totalPermits;

    private final AtomicInteger acquisitions = new AtomicInteger();

    private final AtomicBoolean cancellationRequested = new AtomicBoolean();

    private final CountDownLatch cancellationSignal = new CountDownLatch(1);

    private final CompletableFuture<?> releaseFuture;

    /**
     * @param totalPermits         the total number of permits to release
     * @param releaseOffsetInNanos the offset in nanoseconds from the start time at which
     *                             the given permit (1 to totalPermits) is released
     */
    private ScheduledRateLimiter(int totalPermits, IntToLongFunction releaseOffsetInNanos) {
        this.totalPermits = totalPermits;
        this.releaseFuture = releaseLoop(releaseOffsetInNanos);
    }

    private CompletableFuture<?> releaseLoop(IntToLongFunction releaseOffsetInNanos) {
        final long startNanos = System.nanoTime();

        return CompletableFuture.runAsync(() -> {
            try {
                for (int i = 1; i <= totalPermits; i++) {
                    long delay = startNanos + releaseOffsetInNanos.applyAsLong(i) - System.nanoTime();
                    // Wait on the cancellation signal rather than sleep, so cancel takes effect immediately
                    if (delay > 0 && cancellationSignal.await(delay, TimeUnit.NANOSECONDS)) {
                        break;
                    }
                    semaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).handle((unused, throwable) -> {
            logger.trace("Release loop finished");
            return null;
        });
    }

    /**
     * Attempts to acquire a permit. Blocks the calling thread until the next permit is
     * released if the rate has been reached.
     *
     * @return true if a permit was acquired, or false without blocking if the rate limiter
     * is exhausted (all permits acquired) or cancelled
     * @throws InterruptedException if interrupted while waiting
     */
    @Override
    public boolean acquire() throws InterruptedException {
        if (cancellationRequested.get()) {
            return false;
        }
        // Only the first totalPermits callers wait for a scheduled release
        if (acquisitions.getAndUpdate(n -> n < totalPermits ? n + 1 : n) >= totalPermits) {
            return false;
        }
        semaphore.acquire();
        // Waiters woken by cancellation don't hold a scheduled permit
        return !cancellationRequested.get();
    }

    @Override
    public int getTotalPermits() {
        return totalPermits;
    }

    /**
     * Cancel the rate limiter background thread, effectively voiding the rate limiter.
     * Any callers blocked in {@link #acquire()} are released and return false.
     */
    @Override
    public void cancel() {
        if (cancellationRequested.compareAndSet(false, true)) {
            cancellationSignal.countDown();
            releaseFuture.join();
            // At most totalPermits callers can be waiting, so this wakes them all
            semaphore.release(totalPermits);
        } else {
            logger.trace("Cancellation already completed");
        }
    }

    @Override
    public void close() {
        cancel();
    }
}
