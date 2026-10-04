package io.battery.event;

import io.battery.model.Phase;

public class PhaseProgressEvent extends AbstractProgressEvent<Phase> {
    public PhaseProgressEvent(Object source, Phase phase, double progress) {
        super(source, phase, progress);
    }
}
