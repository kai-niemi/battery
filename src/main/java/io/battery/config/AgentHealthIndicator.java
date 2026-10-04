package io.battery.config;

import java.util.Map;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;
import org.springframework.web.client.ResourceAccessException;

import io.battery.model.Agent;
import io.battery.web.HypermediaClient;
import static io.battery.web.api.model.LinkRelations.ACTUATORS_REL;
import static io.battery.web.api.model.LinkRelations.CURIE_NAMESPACE;
import static org.springframework.hateoas.mediatype.hal.HalLinkRelation.curied;

public class AgentHealthIndicator extends AbstractHealthIndicator {
    private final Agent agent;

    private final HypermediaClient hypermediaClient;

    public AgentHealthIndicator(Agent agent, HypermediaClient hypermediaClient) {
        this.agent = agent;
        this.hypermediaClient = hypermediaClient;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            Map<String, Object> agentBuild = hypermediaClient.from(agent.indexLink())
                    .follow(curied(CURIE_NAMESPACE, ACTUATORS_REL).value())
                    .follow(HalLinkRelation.uncuried("info").value())
                    .toObject("$.build");

            builder.withDetail("build", agentBuild)
                    .withDetail("url", agent.getUrl())
                    .up();
        } catch (ResourceAccessException e) {
            builder.withDetail("url", agent.getUrl())
                    .withDetail("error", e.toString())
                    .down();
        }
    }
}
