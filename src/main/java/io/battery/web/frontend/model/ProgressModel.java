package io.battery.web.frontend.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.battery.model.Phase;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProgressModel {
    private final Map<Phase, Double> phaseHashMap = new HashMap<>();

    public double phaseProgress(Phase phase) {
        return phaseHashMap.getOrDefault(phase, 0.0);
    }

    public void setPhaseProgress(Phase phase, double progress) {
        this.phaseHashMap.put(phase, progress);
    }

    public void reset() {
        this.phaseHashMap.clear();
    }
}
