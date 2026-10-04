package io.battery.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.model.Phase;

@Tag("unit-test")
public class WorkerAdmissionTest {
    private static Phase phase(String name, int maxConcurrency) {
        Phase phase = new Phase();
        phase.setName(name);
        phase.setMaxConcurrency(maxConcurrency);
        return phase;
    }

    @Test
    public void givenMaxConcurrency_expectActiveWorkersCapped() {
        WorkerAdmission workerAdmission = new WorkerAdmission();
        Phase phase = phase("peak", 3);

        List<CompletableFuture<String>> workers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            workerAdmission.admit(phase, CompletableFuture<String>::new).ifPresent(workers::add);
        }

        Assertions.assertThat(workers).hasSize(3);
        Assertions.assertThat(workerAdmission.getActiveWorkers()).isEqualTo(3);
        Assertions.assertThat(workerAdmission.getDroppedWorkers()).containsEntry("peak", 2);
    }

    @Test
    public void givenCompletedWorkers_expectNewWorkersAdmitted() {
        WorkerAdmission workerAdmission = new WorkerAdmission();
        Phase phase = phase("peak", 2);

        CompletableFuture<String> first = workerAdmission.admit(phase, CompletableFuture<String>::new).orElseThrow();
        workerAdmission.admit(phase, CompletableFuture<String>::new).orElseThrow();
        Assertions.assertThat(workerAdmission.admit(phase, CompletableFuture<String>::new)).isEmpty();

        // Both successful and failed workers free their slot
        first.completeExceptionally(new IllegalStateException("test"));

        Assertions.assertThat(workerAdmission.getActiveWorkers()).isEqualTo(1);
        Assertions.assertThat(workerAdmission.admit(phase, CompletableFuture<String>::new)).isPresent();
        Assertions.assertThat(workerAdmission.getDroppedWorkers()).containsEntry("peak", 1);
    }

    @Test
    public void givenWorkersFromEarlierPhase_expectCountedTowardsLimit() {
        WorkerAdmission workerAdmission = new WorkerAdmission();

        for (int i = 0; i < 5; i++) {
            workerAdmission.admit(phase("warmup", 0), CompletableFuture<String>::new).orElseThrow();
        }

        Optional<CompletableFuture<String>> peak
                = workerAdmission.admit(phase("peak", 5), CompletableFuture<String>::new);

        Assertions.assertThat(peak).isEmpty();
        Assertions.assertThat(workerAdmission.getDroppedWorkers())
                .containsOnlyKeys("peak")
                .containsEntry("peak", 1);
    }

    @Test
    public void givenNoMaxConcurrency_expectUnlimited() {
        WorkerAdmission workerAdmission = new WorkerAdmission();
        Phase phase = phase("unlimited", 0);

        for (int i = 0; i < 1000; i++) {
            Assertions.assertThat(workerAdmission.admit(phase, CompletableFuture<String>::new)).isPresent();
        }

        Assertions.assertThat(workerAdmission.getActiveWorkers()).isEqualTo(1000);
        Assertions.assertThat(workerAdmission.getDroppedWorkers()).isEmpty();
    }

    @Test
    public void givenFailingSpawner_expectSlotReleased() {
        WorkerAdmission workerAdmission = new WorkerAdmission();
        Phase phase = phase("peak", 1);

        Assertions.assertThatThrownBy(() -> workerAdmission.admit(phase, () -> {
            throw new IllegalStateException("spawn failed");
        })).isInstanceOf(IllegalStateException.class);

        Assertions.assertThat(workerAdmission.getActiveWorkers()).isZero();
        Assertions.assertThat(workerAdmission.admit(phase, CompletableFuture<String>::new)).isPresent();
    }
}
