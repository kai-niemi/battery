package io.battery.web.frontend.model;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScenarioForm extends RepresentationModel<ScenarioForm> {
    @NotNull(message = "A scenario must be selected")
    private String name;

    private String applicationModelYaml;

    private Boolean skipBeforeSteps;

    private Boolean skipAfterSteps;

    private Boolean skipAllPhases;

    private String[] skipPhases;

    public Boolean getSkipAllPhases() {
        return skipAllPhases;
    }

    public ScenarioForm setSkipAllPhases(Boolean skipAllPhases) {
        this.skipAllPhases = skipAllPhases;
        return this;
    }

    public String getApplicationModelYaml() {
        return applicationModelYaml;
    }

    public void setApplicationModelYaml(String applicationModelYaml) {
        this.applicationModelYaml = applicationModelYaml;
    }

    public String[] getSkipPhases() {
        return skipPhases;
    }

    public void setSkipPhases(String[] skipPhases) {
        this.skipPhases = skipPhases;
    }

    public Boolean getSkipAfterSteps() {
        return skipAfterSteps != null ? skipAfterSteps : false;
    }

    public void setSkipAfterSteps(Boolean skipAfterSteps) {
        this.skipAfterSteps = skipAfterSteps;
    }

    public Boolean getSkipBeforeSteps() {
        return skipBeforeSteps != null ? skipBeforeSteps : false;
    }

    public void setSkipBeforeSteps(Boolean skipBeforeSteps) {
        this.skipBeforeSteps = skipBeforeSteps;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
