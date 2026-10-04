package io.battery.web.api;

import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import io.battery.scenario.worker.Worker;
import io.battery.web.api.model.LinkRelations;
import io.battery.web.api.model.WorkerModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class WorkerResourceAssembler
        extends RepresentationModelAssemblerSupport<Worker, WorkerModel> {
    public WorkerResourceAssembler() {
        super(WorkerRestController.class, WorkerModel.class);
    }

    @Override
    public WorkerModel toModel(Worker worker) {
        WorkerModel resource = new WorkerModel();
        resource.setId(worker.getId());
        resource.setScenario(worker.getScenario());
        resource.setPhase(worker.getPhase());
        resource.setRemainingTime(worker.getRemainingDuration());
        resource.setStatus(worker.getStatus());
        resource.setOpsPerSecond(worker.getMetrics().getOpsPerSec());
        resource.setP90(worker.getMetrics().getP90());
        resource.setP99(worker.getMetrics().getP99());
        resource.setSuccess(worker.getMetrics().getSuccess());
        resource.setTransientErrors(worker.getMetrics().getTransientFail());
        resource.setNonTransientErrors(worker.getMetrics().getNonTransientFail());
        resource.setLastProblem(worker.getLastProblem());

        resource.add(linkTo(methodOn(WorkerRestController.class)
                .getWorker(worker.getId()))
                .withSelfRel()
        );

        if (!worker.isRunning()) {
            resource.add(linkTo(methodOn(WorkerRestController.class)
                    .deleteWorker(worker.getId()))
                    .withRel(LinkRelations.DELETE_REL));
        }

        return resource;
    }
}
