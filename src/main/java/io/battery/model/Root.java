package io.battery.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wraps the model under a {@code battery} key, so that it serializes to the same YAML
 * structure as the configuration it was bound from.
 */
public class Root {
    @JsonProperty("battery")
    private BatteryModel batteryModel;

    public Root(BatteryModel batteryModel) {
        this.batteryModel = batteryModel;
    }

    public BatteryModel getBatteryModel() {
        return batteryModel;
    }
}
