package io.battery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.core.command.completion.CompletionProvider;
import org.springframework.shell.core.command.completion.CompositeCompletionProvider;

import io.battery.model.BatteryModel;
import io.battery.script.BatteryScript;
import io.battery.shell.provider.AgentProvider;
import io.battery.shell.provider.FunctionProvider;
import io.battery.shell.provider.ScenarioProvider;

@Configuration
public class ShellConfig {
    @Bean
    public CompletionProvider scriptFunctionProvider(BatteryScript batteryScript) {
        return new FunctionProvider(batteryScript);
    }

    @Bean
    public CompletionProvider scenarioProvider(BatteryModel battery) {
        return new ScenarioProvider("--scenario", battery);
    }

    @Bean
    public CompletionProvider agentProvider(BatteryModel battery) {
        return new AgentProvider("--name", battery);
    }

    @Bean
    public CompletionProvider agentAndScenarioProvider(BatteryModel battery) {
        return new CompositeCompletionProvider(scenarioProvider(battery), agentProvider(battery));
    }
}
