package io.battery.scenario.worker;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import io.battery.metrics.Metrics;
import io.battery.metrics.MetricsDataPoint;
import io.battery.metrics.Problem;

/**
 * Registry of workers and of sampled time series data points of their call metrics,
 * used for monitoring. Workers stay registered after completion until explicitly deleted.
 * <p>
 * Workers are added from the phase runner thread while being read by monitoring threads,
 * so reads operate on snapshot copies of the synchronized lists rather than streaming them.
 */
@Component
public class WorkTracker {
    private final List<Worker> workers = Collections.synchronizedList(new LinkedList<>());

    private final List<MetricsDataPoint> dataPoints = Collections.synchronizedList(new ArrayList<>());

    public void addWorker(Worker worker) {
        workers.addFirst(worker);
        fireUpdatedEvent();
    }

    public List<Worker> listWorkers() {
        // Copying through the synchronized toArray is safe, unlike streaming the synchronized list
        return Collections.unmodifiableList(new ArrayList<>(workers));
    }

    public List<Worker> listWorkers(Predicate<Worker> predicate) {
        return new ArrayList<>(listWorkers().stream()
                .filter(predicate)
                .toList());
    }

    public List<Worker> listWorkers(int limit, Predicate<Worker> predicate) {
        return new ArrayList<>(listWorkers().stream()
                .filter(predicate)
                .limit(limit)
                .toList());
    }

    public Page<Problem> listProblems(Pageable pageable, Predicate<Worker> predicate) {
        List<Problem> allProblems = listWorkers().stream()
                .filter(predicate)
                .map(Worker::getProblems)
                .flatMap(Collection::stream)
                .toList();

        List<Problem> pageWorkers = new ArrayList<>(allProblems.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList());

        return PageableExecutionUtils.getPage(pageWorkers, pageable, allProblems::size);
    }

    public Page<Worker> listWorkers(Pageable pageable, Predicate<Worker> predicate) {
        // Filter before paging so the total reflects the matching workers
        List<Worker> matchingWorkers = listWorkers(predicate);
        List<Worker> pageWorkers = new ArrayList<>(matchingWorkers.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .toList());
        return PageableExecutionUtils.getPage(pageWorkers, pageable, matchingWorkers::size);
    }

    public Worker getWorkerById(Integer id) {
        return listWorkers()
                .stream()
                .filter(worker -> Objects.equals(worker.getId(), id))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No worker with id: " + id));
    }

    /**
     * @return the id of the closest registered worker before the given id, if any
     */
    public Optional<Integer> findPreviousWorkerId(Integer id) {
        return listWorkers()
                .stream()
                .map(Worker::getId)
                .filter(other -> other < id)
                .max(Integer::compare);
    }

    /**
     * @return the id of the closest registered worker after the given id, if any
     */
    public Optional<Integer> findNextWorkerId(Integer id) {
        return listWorkers()
                .stream()
                .map(Worker::getId)
                .filter(other -> other > id)
                .min(Integer::compare);
    }

    public void deleteAll() {
        workers.removeIf(worker -> !worker.isRunning());
        fireUpdatedEvent();
    }

    public void deleteById(Integer id) {
        Worker worker = getWorkerById(id);
        if (worker.isRunning()) {
            throw new IllegalStateException("Workload is running: " + id);
        }

        workers.remove(worker);

        fireUpdatedEvent();
    }

    private void fireUpdatedEvent() {
    }

    // Time series functions

    public Metrics getMetricsAggregate(Pageable page) {
        List<Metrics> metrics = listWorkers(page, workerModel -> true)
                .stream()
                .map(Worker::getMetrics)
                .toList();
        return Metrics.builder()
                .withUpdateTime(Instant.now())
                .withMeanTimeMillis(metrics.stream()
                        .mapToDouble(Metrics::getMeanTimeMillis).average().orElse(0))
                .withOps(metrics.stream().mapToDouble(Metrics::getOpsPerSec).sum(),
                        metrics.stream().mapToDouble(Metrics::getOpsPerMin).sum())
                .withP50(metrics.stream().mapToDouble(Metrics::getP50).average().orElse(0))
                .withP90(metrics.stream().mapToDouble(Metrics::getP90).average().orElse(0))
                .withP95(metrics.stream().mapToDouble(Metrics::getP95).average().orElse(0))
                .withP99(metrics.stream().mapToDouble(Metrics::getP99).average().orElse(0))
                .withP999(metrics.stream().mapToDouble(Metrics::getP999).average().orElse(0))
                .withMeanTimeMillis(metrics.stream().mapToDouble(Metrics::getMeanTimeMillis).average().orElse(0))
                .withSuccessful(metrics.stream().mapToInt(Metrics::getSuccess).sum())
                .withFails(metrics.stream().mapToInt(Metrics::getTransientFail).sum(),
                        metrics.stream().mapToInt(Metrics::getNonTransientFail).sum())
                .build();
    }

    public void takeSnapshot() {
        int samplePeriodSeconds = 300;

        Duration samplePeriod = Duration.ofSeconds(samplePeriodSeconds);

        // Purge old data points older than sample period
        dataPoints.removeIf(item -> item.getInstant()
                .isBefore(Instant.now().minusSeconds(samplePeriod.toSeconds())));

        // Add new datapoint by sampling all worker metrics
        MetricsDataPoint dataPoint = new MetricsDataPoint(Instant.now());

        // Add datapoint if still running
        listWorkers()
                .stream()
                .filter(Worker::isRunning)
                .forEach(worker -> dataPoint.putValue(worker.getId(), worker.getMetrics()));

        dataPoints.add(dataPoint);
    }

    public List<Map<String, Object>> getDataPoints(Function<Metrics, Double> mapper, Pageable page) {
        final List<Map<String, Object>> columnData = new ArrayList<>();

        // Snapshot so that labels and values are consistent while snapshots are taken concurrently
        final List<MetricsDataPoint> dataPoints = new ArrayList<>(this.dataPoints);

        {
            final Map<String, Object> headerElement = new HashMap<>();
            List<Long> labels = dataPoints
                    .stream()
                    .map(MetricsDataPoint::getInstant)
                    .toList()
                    .stream()
                    .map(Instant::toEpochMilli)
                    .toList();
            headerElement.put("data", labels.toArray());
            columnData.add(headerElement);
        }

        listWorkers(page, (x) -> true)
                .forEach(worker -> {
                    Map<String, Object> dataElement = new HashMap<>();

                    List<Metrics> metrics = new ArrayList<>();

                    dataPoints.forEach(dataPoint -> metrics.add(
                            dataPoint.getValue(worker.getId(), Metrics.empty())));

                    List<Double> data = metrics
                            .stream()
                            .map(mapper)
                            .toList();

                    dataElement.put("id", worker.getId());
                    dataElement.put("name", "%s (%d)".formatted(worker.getScenario(), worker.getId()));
                    dataElement.put("data", data.toArray());

                    columnData.add(dataElement);
                });

        return columnData;
    }
}
