package io.battery.web.api.model;

import org.springframework.hateoas.IanaLinkRelations;

public abstract class LinkRelations {
    private LinkRelations() {
    }

    public static final String SCENARIO_REL = "scenario";

    public static final String SCENARIOS_REL = "scenarios";

    public static final String WORKER_REL = "worker";

    public static final String WORKERS_REL = "workers";

    public static final String WORKERS_DELETE_REL = "workers-delete";

    public static final String PROBLEMS_REL = "problems";

    public static final String RUN_REL = "run";

    public static final String RUNS_REL = "runs";

    public static final String LATEST_RUN_REL = "latest-run";

    public static final String CURRENT_REL = IanaLinkRelations.CURRENT_VALUE;

    public static final String START_REL = IanaLinkRelations.START_VALUE;

    public static final String STATUS_REL = "status";

    public static final String ABORT_REL = "abort";

    public static final String DELETE_REL = "delete";

    public static final String ACTUATORS_REL = "actuators";

    // IANA standard link relations:
    // http://www.iana.org/assignments/link-relations/link-relations.xhtml

    public static final String CURIE_NAMESPACE = "battery";
}
