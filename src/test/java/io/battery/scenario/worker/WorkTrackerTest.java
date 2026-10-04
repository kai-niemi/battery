package io.battery.scenario.worker;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Tag("unit-test")
public class WorkTrackerTest {
    private static Worker worker(int id) {
        return new Worker(id, "scenario", "phase", Duration.ofMinutes(1));
    }

    @Test
    public void givenFilteredPage_expectTotalOfMatchingWorkers() {
        WorkTracker workTracker = new WorkTracker();
        for (int i = 1; i <= 10; i++) {
            Worker worker = worker(i);
            if (i % 2 == 0) {
                worker.markCompleted();
            }
            workTracker.addWorker(worker);
        }

        Page<Worker> page = workTracker.listWorkers(PageRequest.of(0, 3), Worker::isRunning);

        Assertions.assertThat(page.getContent()).hasSize(3).allMatch(Worker::isRunning);
        Assertions.assertThat(page.getTotalElements()).isEqualTo(5);
        Assertions.assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    public void givenConcurrentAdditions_expectReadsWithoutConcurrentModification() throws Exception {
        WorkTracker workTracker = new WorkTracker();
        AtomicBoolean done = new AtomicBoolean();

        // Added up front so the reader can look it up before the writer starts
        workTracker.addWorker(worker(1));

        CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
            for (int i = 2; i <= 20_000; i++) {
                workTracker.addWorker(worker(i));
                if (i % 100 == 0) {
                    workTracker.takeSnapshot();
                }
            }
            done.set(true);
        });

        // Read with each streaming method while workers and data points are being added
        while (!done.get()) {
            workTracker.listWorkers(Worker::isRunning);
            workTracker.listWorkers(10, Worker::isRunning);
            workTracker.listWorkers(PageRequest.of(0, 10), worker -> true);
            workTracker.getWorkerById(1);
            workTracker.getDataPoints(metrics -> metrics.getMeanTimeMillis(), PageRequest.of(0, 10));
        }

        writer.get(30, TimeUnit.SECONDS);
        Assertions.assertThat(workTracker.listWorkers()).hasSize(20_000);
    }
}
