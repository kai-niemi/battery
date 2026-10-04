package io.battery.web.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.battery.ProfileNames;
import io.battery.metrics.Metrics;
import io.battery.metrics.TimeSeries;
import io.battery.scenario.worker.WorkTracker;
import io.battery.web.api.model.MessageModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Chart JS data paint callback methods.
 * For internal use only, thus not exposed via the API root.
 */
@RestController
@Profile(value = ProfileNames.ONLINE)
@RequestMapping(value = "/api/chart")
public class ChartRestController {
    @Autowired
    private WorkTracker workTracker;

    @Autowired
    @Qualifier("threadPoolTimeSeries")
    private TimeSeries threadPoolTimeSeries;

    @Autowired
    @Qualifier("connectionPoolTimeSeries")
    private TimeSeries connectionPoolTimeSeries;

    @Autowired
    @Qualifier("connectionPoolTimingTimeSeries")
    private TimeSeries connectionPoolTimingTimeSeries;

    @Autowired
    @Qualifier("cpuTimeSeries")
    private TimeSeries cpuTimeSeries;

    @Autowired
    @Qualifier("workerTimeSeries")
    private TimeSeries workerTimeSeries;

    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void takeDataPointSnapshots() {
        workTracker.takeSnapshot();
        threadPoolTimeSeries.takeSnapshot();
        connectionPoolTimeSeries.takeSnapshot();
        connectionPoolTimingTimeSeries.takeSnapshot();
        cpuTimeSeries.takeSnapshot();
        workerTimeSeries.takeSnapshot();
    }

    @GetMapping
    public ResponseEntity<MessageModel> index() {
        MessageModel index = new MessageModel();
        index.add(linkTo(methodOn(getClass())
                .index())
                .withSelfRel());
        index.add(linkTo(methodOn(getClass())
                .getConnectionPoolDataPoints())
                .withRel("data-points"));
        index.add(linkTo(methodOn(getClass())
                .getConnectionPoolTimingsDataPoints())
                .withRel("data-points"));
        index.add(linkTo(methodOn(getClass())
                .getThreadPoolDataPoints())
                .withRel("data-points"));
        index.add(linkTo(methodOn(getClass())
                .getCpuDataPoints())
                .withRel("data-points"));
        index.add(linkTo(methodOn(getClass())
                .getWorkloadDataPoints())
                .withRel("data-points"));
        return ResponseEntity.ok(index);
    }

    @GetMapping(value = "connection-pool",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getConnectionPoolDataPoints() {
        return connectionPoolTimeSeries.getDataPoints();
    }

    @GetMapping(value = "connection-pool-timings",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getConnectionPoolTimingsDataPoints() {
        return connectionPoolTimingTimeSeries.getDataPoints();
    }

    @GetMapping(value = "thread-pool",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getThreadPoolDataPoints() {
        return threadPoolTimeSeries.getDataPoints();
    }

    @GetMapping(value = "cpu",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getCpuDataPoints() {
        return cpuTimeSeries.getDataPoints();
    }

    @GetMapping(value = "workers",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPoints() {
        return workerTimeSeries.getDataPoints();
    }

    //
    // Special worker aggregation metrics (not using micrometer)
    //

    @GetMapping(value = "workers/p99",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPointsP99(Pageable page) {
        return workTracker.getDataPoints(Metrics::getP99, page);
    }

    @GetMapping(value = "workers/p999",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPointsP999(Pageable page) {
        return workTracker.getDataPoints(Metrics::getP999, page);
    }

    @GetMapping(value = "workers/tps",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody List<Map<String, Object>> getWorkloadDataPointsTPS(Pageable page) {
        return workTracker.getDataPoints(Metrics::getOpsPerSec, page);
    }
}
