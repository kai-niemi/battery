package io.battery.shell.provider;

import java.util.ArrayList;
import java.util.List;

import org.springframework.shell.core.command.completion.CompletionContext;
import org.springframework.shell.core.command.completion.CompletionProposal;
import org.springframework.shell.core.command.completion.CompletionProvider;

import io.battery.model.Agent;
import io.battery.model.BatteryModel;

public class AgentProvider implements CompletionProvider {
    private final String prefix;

    private final BatteryModel battery;

    public AgentProvider(String prefix, BatteryModel battery) {
        this.prefix = prefix;
        this.battery = battery;
    }

    @Override
    public List<CompletionProposal> apply(CompletionContext completionContext) {
        List<CompletionProposal> result = new ArrayList<>();

        for (Agent agent : battery.getNetwork().getAgents()) {
            String prefix = completionContext.currentWordUpToCursor();
            if (prefix == null) {
                prefix = "";
            }
            if (agent.getUrl().startsWith(prefix)) {
                result.add(new CompletionProposal(this.prefix + "=" + agent.getName())
                        .displayText(agent.getName())
                        .description(agent.getUrl()));
            }
        }

        return result;
    }
}
