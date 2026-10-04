package io.battery.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;

/**
 * The remote {@link Agent agents} that this instance can control.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Network {
    private List<@Valid Agent> agents = new ArrayList<>();

    public Stream<Agent> findAgents(String name) {
        if (StringUtils.hasText(name)) {
            return Stream.of(agents.stream()
                    .filter(agent -> agent.getName().equals(name))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + name)));
        }
        return agents.stream();
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }
}
