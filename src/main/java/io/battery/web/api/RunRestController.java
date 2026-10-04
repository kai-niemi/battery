package io.battery.web.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.battery.ProfileNames;
import io.battery.scenario.run.RunRecorder;
import io.battery.scenario.run.RunSummary;
import io.battery.web.api.model.LinkRelations;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Summaries of the most recent scenario runs, including key metrics, findings and the
 * whole-run latency histogram for merging runs, such as those of several agents.
 */
@RestController
@Profile(value = ProfileNames.ONLINE)
@RequestMapping(value = "/api/run")
public class RunRestController {
    @Autowired
    private RunRecorder runRecorder;

    @GetMapping
    public HttpEntity<CollectionModel<EntityModel<RunSummary>>> findRuns() {
        List<EntityModel<RunSummary>> runs = runRecorder.getRecentRuns().stream()
                .map(this::toModel)
                .toList();
        CollectionModel<EntityModel<RunSummary>> model = CollectionModel.of(runs)
                .add(linkTo(methodOn(RunRestController.class).findRuns())
                        .withSelfRel())
                .add(linkTo(methodOn(RunRestController.class).findLatestRun())
                        .withRel(LinkRelations.LATEST_RUN_REL)
                        .withTitle("Most recent run"));
        return ResponseEntity.ok(model);
    }

    @GetMapping("/latest")
    public HttpEntity<EntityModel<RunSummary>> findLatestRun() {
        return runRecorder.getLastRun()
                .map(summary -> ResponseEntity.ok(toModel(summary)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public HttpEntity<EntityModel<RunSummary>> findRun(@PathVariable("id") Integer id) {
        return runRecorder.getRun(id)
                .map(summary -> ResponseEntity.ok(toModel(summary)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private EntityModel<RunSummary> toModel(RunSummary summary) {
        return EntityModel.of(summary)
                .add(linkTo(methodOn(RunRestController.class).findRun(summary.id()))
                        .withSelfRel())
                .add(linkTo(methodOn(RunRestController.class).findRuns())
                        .withRel(LinkRelations.RUNS_REL));
    }
}
