package io.battery.web.api.model;

public class StartScenarioForm {
    private String name;

    private String secureHash;

    private boolean skipBeforeSteps;

    private boolean skipAfterSteps;

    private boolean skipPhases;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getSecureHash() {
        return secureHash;
    }

    public void setSecureHash(String secureHash) {
        this.secureHash = secureHash;
    }

    public boolean isSkipBeforeSteps() {
        return skipBeforeSteps;
    }

    public void setSkipBeforeSteps(boolean skipBeforeSteps) {
        this.skipBeforeSteps = skipBeforeSteps;
    }

    public boolean isSkipAfterSteps() {
        return skipAfterSteps;
    }

    public void setSkipAfterSteps(boolean skipAfterSteps) {
        this.skipAfterSteps = skipAfterSteps;
    }

    public boolean isSkipPhases() {
        return skipPhases;
    }

    public void setSkipPhases(boolean skipPhases) {
        this.skipPhases = skipPhases;
    }
}
