package io.battery.web.api;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.battery.ProfileNames;
import io.battery.metrics.Problem;
import io.battery.scenario.worker.WorkTracker;
import io.battery.scenario.worker.Worker;
import io.battery.web.api.model.LinkRelations;
import io.battery.web.api.model.WorkerModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@Profile(value = ProfileNames.ONLINE)
@RequestMapping(value = "/api/worker")
public class WorkerRestController {
    @Autowired
    private WorkTracker workTracker;

    @Autowired
    private PagedResourcesAssembler<Worker> workerPagedResourcesAssembler;

    @Autowired
    private PagedResourcesAssembler<Problem> problemPagedResourcesAssembler;

    @Autowired
    private WorkerResourceAssembler workerResourceAssembler;

    @GetMapping
    public HttpEntity<PagedModel<WorkerModel>> findWorkers(
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size) {
        PageRequest pageRequest = PageRequest.of(page.orElse(0), size.orElse(10));

        PagedModel<WorkerModel> resource = workerPagedResourcesAssembler
                .toModel(workTracker.listWorkers(pageRequest, worker -> true), workerResourceAssembler);

        resource.add(linkTo(methodOn(WorkerRestController.class)
                        .findWorkers(page, size))
                        .withSelfRel())
                .add(linkTo(methodOn(WorkerRestController.class)
                        .findProblems(page, size))
                        .withRel(LinkRelations.PROBLEMS_REL))
                .add(linkTo(methodOn(WorkerRestController.class)
                        .deleteAllWorkers())
                        .withRel(LinkRelations.WORKERS_DELETE_REL));

        return ResponseEntity.ok(resource);
    }

    @GetMapping("/problems")
    public HttpEntity<PagedModel<EntityModel<Problem>>> findProblems(
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size) {
        PageRequest pageRequest = PageRequest.of(page.orElse(0), size.orElse(10));
        PagedModel<EntityModel<Problem>> pagedModel = problemPagedResourcesAssembler
                .toModel(workTracker.listProblems(pageRequest, worker -> true));
        pagedModel.add(linkTo(methodOn(WorkerRestController.class)
                .findWorkers(page, size))
                .withSelfRel());
        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping(value = "/{id}")
    public HttpEntity<WorkerModel> getWorker(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(workerResourceAssembler.toModel(workTracker.getWorkerById(id)));
    }

    @PostMapping(value = "/{id}/delete")
    @DeleteMapping(value = "/{id}/delete")
    public HttpEntity<Void> deleteWorker(@PathVariable("id") Integer id) {
        workTracker.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/delete")
    @DeleteMapping(value = "/delete")
    public HttpEntity<Void> deleteAllWorkers() {
        workTracker.deleteAll();
        return ResponseEntity.ok().build();
    }
}
