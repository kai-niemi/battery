package io.battery.web.frontend;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import io.battery.ProfileNames;
import io.battery.event.PhaseProgressEvent;
import io.battery.metrics.Metrics;
import io.battery.scenario.worker.WorkTracker;
import io.battery.scenario.worker.Worker;
import io.battery.scenario.worker.WorkerStatus;
import io.battery.web.frontend.model.FilterForm;
import io.battery.web.frontend.model.TopicName;

@WebController
@Profile(value = ProfileNames.ONLINE)
@RequestMapping("/worker")
public class WorkerController {
    @Autowired
    private WorkTracker workTracker;

    @Autowired
    private SimpMessagePublisher simpMessagePublisher;

    @Scheduled(fixedRate = 6, initialDelay = 6, timeUnit = TimeUnit.SECONDS)
    public void chartUpdate() {
        simpMessagePublisher.convertAndSend(TopicName.WORKER_MODEL_UPDATE, null);
    }

    private void populateWorkerModel(WorkerStatus status, Pageable page, Model model) {
        Page<Worker> workerPage = workTracker.listWorkers(page, worker -> {
            if (Objects.nonNull(status)) {
                return status.equals(worker.getStatus());
            }
            return true;
        });

        model.addAttribute("form", new FilterForm().setStatus(status));
        model.addAttribute("workerPage", workerPage);
        model.addAttribute("aggregatedMetrics", workTracker.getMetricsAggregate(page));
    }

    @GetMapping
    public Callable<String> indexPage(
            @RequestParam(value = "status", required = false) WorkerStatus status,
            @PageableDefault(size = 15) Pageable page,
            Model model) {
        return () -> {
            populateWorkerModel(status, page, model);
            return "worker";
        };
    }

    @GetMapping("/table-rows")
    public Callable<String> tableRows(
            @RequestParam(value = "status", required = false) WorkerStatus status,
            @PageableDefault(size = 15) Pageable page,
            Model model) {
        return () -> {
            populateWorkerModel(status, page, model);
            return "worker :: workerRows";
        };
    }

    @GetMapping("/paging-banner")
    public Callable<String> pagingBanner(
            @RequestParam(value = "status", required = false) WorkerStatus status,
            @PageableDefault(size = 15) Pageable page,
            Model model) {
        return () -> {
            populateWorkerModel(status, page, model);
            return "worker :: pagingBanner";
        };
    }

    @GetMapping("/{id}")
    public Callable<String> detailsPage(@PathVariable("id") Integer id, Model model) {
        return () -> {
            Worker worker = workTracker.getWorkerById(id);
            model.addAttribute("form", worker);
            model.addAttribute("previousId", workTracker.findPreviousWorkerId(id).orElse(null));
            model.addAttribute("nextId", workTracker.findNextWorkerId(id).orElse(null));
            return "worker-detail";
        };
    }

    @PostMapping(value = "/deleteAll")
    public RedirectView deleteAll() {
        workTracker.deleteAll();
        return new RedirectView("/worker");
    }

    @GetMapping(value = "/delete/{id}")
    public RedirectView delete(@PathVariable("id") Integer id) {
        workTracker.deleteById(id);
        return new RedirectView("/worker");
    }

    @GetMapping("/items")
    public @ResponseBody List<Worker> getWorkloadItems(Pageable page) {
        return workTracker.listWorkers(page, (x) -> true).getContent();
    }

    @GetMapping("/summary")
    public @ResponseBody Metrics getWorkloadSummary(Pageable page) {
        return workTracker.getMetricsAggregate(page);
    }
}
