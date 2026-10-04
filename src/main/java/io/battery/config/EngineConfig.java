package io.battery.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.battery.ProfileNames;
import io.battery.model.Agent;
import io.battery.model.BatteryModel;
import io.battery.scenario.step.SimpleStepRunner;
import io.battery.scenario.step.StepActionProvider;
import io.battery.scenario.step.StepRunner;
import io.battery.script.BatteryScript;
import io.battery.web.HypermediaClient;

@Configuration
public class EngineConfig {
    @Bean
    public BatteryScript batteryScript(@Autowired DataSource dataSource) {
        BatteryScript batteryScript = new BatteryScript();
        batteryScript.registerStandardFunctions(dataSource);
        return batteryScript;
    }

    @Bean
    public StepRunner stepRunner(StepActionProvider stepActionProvider) {
        // Steps run on the calling worker thread so that they participate in any
        // thread-bound transaction of transactional scenarios
        return new SimpleStepRunner(stepActionProvider);
    }

    @Bean
    @Profile(value = ProfileNames.ONLINE)
    public HealthContributor agentHealthContributor(BatteryModel batteryModel,
                                                    HypermediaClient hypermediaClient) {

        Map<String, Agent> beans = new HashMap<>();
        batteryModel.getNetwork().getAgents().forEach(
                agent -> beans.put(agent.getName(), agent));
        return CompositeHealthContributor.fromMap(beans, agent ->
                new AgentHealthIndicator(agent, hypermediaClient));
    }
}
