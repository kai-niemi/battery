package io.battery.web.api.model;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.battery.scenario.ScenarioStatus;

@JsonPropertyOrder({"links", "embedded", "templates"})
public class StatusModel extends RepresentationModel<StatusModel> {
    private String appName;

    private String appVersion;

    private String secureHash;

    private ScenarioStatus scenarioStatus;

    public String getAppName() {
        return appName;
    }

    public StatusModel setAppName(String appName) {
        this.appName = appName;
        return this;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public StatusModel setAppVersion(String appVersion) {
        this.appVersion = appVersion;
        return this;
    }

    public String getSecureHash() {
        return secureHash;
    }

    public StatusModel setSecureHash(String secureHash) {
        this.secureHash = secureHash;
        return this;
    }

    public ScenarioStatus getScenarioStatus() {
        return scenarioStatus;
    }

    public StatusModel setScenarioStatus(ScenarioStatus scenarioStatus) {
        this.scenarioStatus = scenarioStatus;
        return this;
    }
}
