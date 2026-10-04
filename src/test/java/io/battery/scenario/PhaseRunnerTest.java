package io.battery.scenario;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import io.battery.model.Phase;

@Tag("unit-test")
public class PhaseRunnerTest {
    private static Phase phase(String name, int startRate, int maxRate, Duration duration) {
        Phase phase = new Phase();
        phase.setName(name);
        phase.setStartRate(startRate);
        phase.setMaxRate(maxRate);
        phase.setDuration(duration);
        return phase;
    }

    @Test
    public void givenInterruptedThread_expectRemainingPhasesCancelled() {
        PhaseRunner phaseRunner = new PhaseRunner();
        ReflectionTestUtils.setField(phaseRunner, "applicationEventPublisher",
                (ApplicationEventPublisher) event -> {
                });

        CancellationMarker cancellationMarker = new CancellationMarker();
        AtomicInteger workers = new AtomicInteger();

        List<Phase> phases = List.of(
                phase("ramp", 0, 10, Duration.ofSeconds(5)),
                phase("pause", 0, 0, Duration.ofSeconds(5)),
                phase("ramp-again", 1, 10, Duration.ofSeconds(5)));

        // Interrupt while the first phase (ramping from zero) is blocked waiting for its first permit at 1s
        Thread runnerThread = Thread.currentThread();
        CompletableFuture.delayedExecutor(200, TimeUnit.MILLISECONDS).execute(runnerThread::interrupt);

        Instant startTime = Instant.now();
        try {
            Assertions.assertThatThrownBy(() -> phaseRunner.runPhases(phases,
                            (phase, permits) -> workers.incrementAndGet(), cancellationMarker))
                    .isInstanceOf(ScenarioCancellationException.class)
                    .hasMessageContaining("Cancelling phase [ramp]");

            Assertions.assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            // Clear the interrupt flag for subsequent tests
            Thread.interrupted();
        }

        Assertions.assertThat(cancellationMarker.check()).isTrue();
        Assertions.assertThat(cancellationMarker.getReason()).contains("interrupted");
        Assertions.assertThat(workers).hasValue(0);
        Assertions.assertThat(Duration.between(startTime, Instant.now())).isLessThan(Duration.ofSeconds(1));
    }
}
