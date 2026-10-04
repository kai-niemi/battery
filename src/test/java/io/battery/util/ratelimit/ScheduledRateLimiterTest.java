package io.battery.util.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("integration-test")
public class ScheduledRateLimiterTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void givenFixedPeriod_expectRateLimiting() throws InterruptedException {
        // Creates 20 virtual users in 60 seconds (one virtual user approximately every 3 seconds)
        try (RateLimiter rateLimiter = ScheduledRateLimiter.fixed(20, Duration.ofSeconds(60))) {
            Instant startTime = Instant.now();

            int permits = 0;
            while (rateLimiter.acquire()) {
                logger.info("Acquired %d at %s".formatted(++permits, Instant.now()));
            }

            Assertions.assertThat(permits).isEqualTo(20);
            Assertions.assertThat(Duration.between(startTime, Instant.now()))
                    .isBetween(Duration.ofMillis(59_500), Duration.ofMillis(60_500));
        }
    }

    @Test
    public void givenSubMillisecondInterval_expectReleaseOverFullPeriod() throws InterruptedException {
        // 2000 permits in 1 second is one permit every 0.5ms
        try (RateLimiter rateLimiter = ScheduledRateLimiter.fixed(2000, Duration.ofSeconds(1))) {
            Instant startTime = Instant.now();

            for (int i = 0; i < 1000; i++) {
                rateLimiter.acquire();
            }
            Duration halfway = Duration.between(startTime, Instant.now());

            for (int i = 0; i < 1000; i++) {
                rateLimiter.acquire();
            }
            Duration total = Duration.between(startTime, Instant.now());

            logger.info("Acquired 1000 after %s and 2000 after %s".formatted(halfway, total));

            Assertions.assertThat(halfway).isBetween(Duration.ofMillis(400), Duration.ofMillis(600));
            Assertions.assertThat(total).isBetween(Duration.ofMillis(950), Duration.ofMillis(1100));
        }
    }

    @Test
    public void givenExhaustedPermits_expectNonBlockingAcquire() throws InterruptedException {
        try (RateLimiter rateLimiter = ScheduledRateLimiter.fixed(10, Duration.ofMillis(500))) {
            for (int i = 0; i < 10; i++) {
                Assertions.assertThat(rateLimiter.acquire()).isTrue();
            }

            Instant startTime = Instant.now();
            for (int i = 0; i < 1000; i++) {
                Assertions.assertThat(rateLimiter.acquire()).isFalse();
            }

            Assertions.assertThat(Duration.between(startTime, Instant.now())).isLessThan(Duration.ofMillis(50));
        }
    }

    @Test
    public void givenBlockedAcquire_whenCancelled_expectRelease() throws Exception {
        // One permit after 10 seconds, so the caller is blocked when cancelled
        RateLimiter rateLimiter = ScheduledRateLimiter.fixed(1, Duration.ofSeconds(10));

        CompletableFuture<Boolean> blocked = CompletableFuture.supplyAsync(() -> {
            try {
                return rateLimiter.acquire();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });

        TimeUnit.MILLISECONDS.sleep(200);
        Assertions.assertThat(blocked).isNotDone();

        Instant cancelTime = Instant.now();
        rateLimiter.cancel();

        Assertions.assertThat(blocked.get(1, TimeUnit.SECONDS)).isFalse();
        Assertions.assertThat(Duration.between(cancelTime, Instant.now())).isLessThan(Duration.ofMillis(500));
        Assertions.assertThat(rateLimiter.acquire()).isFalse();
    }

    @Test
    public void givenLinearRampPeriod_expectRateLimiting() throws InterruptedException {
        try (RateLimiter rateLimiter = ScheduledRateLimiter.linearRamp(1, 8, Duration.ofSeconds(30))) {
            Instant startTime = Instant.now();

            int acquisitions = 0;
            int firstTenSeconds = 0;
            int lastTenSeconds = 0;
            while (rateLimiter.acquire()) {
                acquisitions++;
                Duration elapsed = Duration.between(startTime, Instant.now());
                if (elapsed.compareTo(Duration.ofSeconds(10)) < 0) {
                    firstTenSeconds++;
                } else if (elapsed.compareTo(Duration.ofSeconds(20)) >= 0) {
                    lastTenSeconds++;
                }
                logger.info("Acquired total %d after %s".formatted(acquisitions, elapsed));
            }

            // (1 + 8) / 2 permits/sec over 30 seconds, ramping from ~21 to ~68 permits per 10 seconds
            Assertions.assertThat(acquisitions).isEqualTo(135);
            Assertions.assertThat(Duration.between(startTime, Instant.now()))
                    .isBetween(Duration.ofMillis(29_500), Duration.ofMillis(30_500));
            Assertions.assertThat(firstTenSeconds).isBetween(18, 24);
            Assertions.assertThat(lastTenSeconds).isBetween(65, 71);
        }
    }

    @Test
    public void givenConstantRatePeriod_expectRateLimiting() throws InterruptedException {
        try (RateLimiter rateLimiter = ScheduledRateLimiter.linearRamp(2, 2, Duration.ofSeconds(30))) {
            Instant startTime = Instant.now();

            int acquisitions = 0;
            while (rateLimiter.acquire()) {
                acquisitions++;
                logger.info("Acquired total %d after %s".formatted(acquisitions,
                        Duration.between(startTime, Instant.now())));
            }

            Assertions.assertThat(acquisitions).isEqualTo(60);
            Assertions.assertThat(Duration.between(startTime, Instant.now()))
                    .isBetween(Duration.ofMillis(29_500), Duration.ofMillis(30_500));
        }
    }

    @Test
    public void givenRampFromZero_expectRateLimiting() throws InterruptedException {
        try (RateLimiter rateLimiter = ScheduledRateLimiter.linearRamp(0, 10, Duration.ofSeconds(4))) {
            Instant startTime = Instant.now();

            int acquisitions = 0;
            int firstTwoSeconds = 0;
            while (rateLimiter.acquire()) {
                acquisitions++;
                if (Duration.between(startTime, Instant.now()).compareTo(Duration.ofSeconds(2)) < 0) {
                    firstTwoSeconds++;
                }
            }

            // 10 / 2 permits/sec over 4 seconds, with a quarter of them in the first half
            Assertions.assertThat(acquisitions).isEqualTo(20);
            Assertions.assertThat(firstTwoSeconds).isBetween(4, 6);
            Assertions.assertThat(Duration.between(startTime, Instant.now()))
                    .isBetween(Duration.ofMillis(3_800), Duration.ofMillis(4_200));
        }
    }

    @Test
    public void givenRampPeriod_expectPermitsReleasedWithoutBursts() throws InterruptedException {
        try (RateLimiter rateLimiter = ScheduledRateLimiter.linearRamp(10, 10, Duration.ofSeconds(2))) {
            Instant startTime = Instant.now();

            // 10 permits/sec is one permit every 100ms rather than 10 permits at the start of each second
            for (int i = 0; i < 10; i++) {
                Assertions.assertThat(rateLimiter.acquire()).isTrue();
            }

            Assertions.assertThat(Duration.between(startTime, Instant.now())).isGreaterThan(Duration.ofMillis(900));
        }
    }

    @Test
    public void givenElapsedRampPeriod_expectNonBlockingAcquire() throws InterruptedException {
        try (RateLimiter rateLimiter = ScheduledRateLimiter.linearRamp(1, 2, Duration.ofSeconds(3))) {
            Instant startTime = Instant.now();

            int acquisitions = 0;
            while (rateLimiter.acquire()) {
                acquisitions++;
            }

            Instant exhaustedTime = Instant.now();
            for (int i = 0; i < 1000; i++) {
                Assertions.assertThat(rateLimiter.acquire()).isFalse();
            }

            // (1 + 2) / 2 permits/sec over 3 seconds
            Assertions.assertThat(acquisitions).isEqualTo(4);
            Assertions.assertThat(Duration.between(startTime, exhaustedTime)).isLessThan(Duration.ofMillis(3200));
            Assertions.assertThat(Duration.between(exhaustedTime, Instant.now())).isLessThan(Duration.ofMillis(50));
        }
    }
}
