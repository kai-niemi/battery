package io.battery.shell.provider;

import java.util.ArrayList;
import java.util.List;

import org.springframework.shell.core.command.completion.CompletionContext;
import org.springframework.shell.core.command.completion.CompletionProposal;
import org.springframework.shell.core.command.completion.CompletionProvider;

import io.battery.model.BatteryModel;
import io.battery.model.Scenario;

public class ScenarioProvider implements CompletionProvider {
    private final String prefix;

    private final BatteryModel battery;

    public ScenarioProvider(String prefix, BatteryModel battery) {
        this.prefix = prefix;
        this.battery = battery;
    }

    @Override
    public List<CompletionProposal> apply(CompletionContext completionContext) {
        List<CompletionProposal> result = new ArrayList<>();

        for (Scenario scenario : battery.getScenarios()) {
            String prefix = completionContext.currentWordUpToCursor();
            if (prefix == null) {
                prefix = "";
            }
            if (scenario.getName().startsWith(prefix)) {
                result.add(new CompletionProposal(
                        this.prefix + "=\"" + scenario.getName() + "\"")
                        .description(scenario.describe()));
            }
        }

        return result;
    }
}
