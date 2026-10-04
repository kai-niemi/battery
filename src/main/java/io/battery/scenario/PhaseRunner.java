package io.battery.scenario;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import io.battery.event.PhaseProgressEvent;
import io.battery.model.Phase;
import io.battery.util.ratelimit.RateLimiter;
import io.battery.util.ratelimit.ScheduledRateLimiter;

/**
 * Support bean running the battery model phases in sequence, invoking a callback for
 * each permit released by the phase rate limiter, or pausing for pause phases.
 * Stops with a {@link ScenarioCancellationException} on cancellation or interruption.
 */
@Component
public class PhaseRunner {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * @param phases      the phases controlling the rate at which userAction callback is invoked
     * @param phaseAction the user action representing a unit-of-work for a single virtual user
     */
    public void runPhases(List<Phase> phases, BiConsumer<Phase, Integer> phaseAction,
                          CancellationMarker cancellationMarker) {
        runPhases(phases, phaseAction, cancellationMarker, phase -> {
        });
    }

    /**
     * @param phaseStarted called when each phase starts, before any of its users are created
     */
    public void runPhases(List<Phase> phases, BiConsumer<Phase, Integer> phaseAction,
                          CancellationMarker cancellationMarker, Consumer<Phase> phaseStarted) {
        for (Phase phase : phases) {
            checkCancellation(phase, cancellationMarker);

            final Instant now = Instant.now();
            phaseStarted.accept(phase);

            if (phase.isPausePhase()) {
                logger.debug("Pausing phase [%s]".formatted(phase.describe()));
                pausePhase(phase);
            } else {
                logger.debug("Ramping phase [%s]".formatted(phase.describe()));
                rampPhase(phase, phaseAction, cancellationMarker);
            }

            checkCancellation(phase, cancellationMarker);

            logger.debug("Completed phase [%s] in %s"
                    .formatted(phase.getName(), Duration.between(now, Instant.now())));
        }
    }

    private void checkCancellation(Phase phase, CancellationMarker cancellationMarker) {
        // An interrupted phase returns early with the interrupt flag restored, so treat it as
        // a cancellation rather than running the remaining phases into immediate interrupts
        if (Thread.currentThread().isInterrupted() && !cancellationMarker.check()) {
            cancellationMarker.markCancelled("Thread interrupted during phase [%s]".formatted(phase.getName()));
        }
        if (cancellationMarker.check()) {
            throw new ScenarioCancellationException(phase, cancellationMarker);
        }
    }

    private void rampPhase(Phase phase,
                           BiConsumer<Phase, Integer> phaseConsumer,
                           CancellationMarker cancellationMarker) {
        try (RateLimiter rateLimiter = phase.isFixedPhase()
                ? ScheduledRateLimiter.fixed(phase.getUsers(), phase.getDuration())
                : ScheduledRateLimiter.linearRamp(phase.getStartRate(), phase.getEffectiveMaxRate(),
                phase.getDuration())) {

            int permits = 0;

            // The rate limiter owns the phase termination by becoming exhausted
            while (rateLimiter.acquire()) {
                if (cancellationMarker.check()) {
                    throw new ScenarioCancellationException(phase, cancellationMarker);
                }

                permits++;

                final double completion = ((double) permits / (double) rateLimiter.getTotalPermits()) * 100.0;
                applicationEventPublisher.publishEvent(new PhaseProgressEvent(this, phase, completion));

                phaseConsumer.accept(phase, permits);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void pausePhase(Phase phase) {
        try {
            applicationEventPublisher.publishEvent(new PhaseProgressEvent(this, phase, 0.0));
            TimeUnit.MILLISECONDS.sleep(phase.getDuration().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            applicationEventPublisher.publishEvent(new PhaseProgressEvent(this, phase, 100));
        }
    }
}
