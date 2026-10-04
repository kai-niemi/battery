package io.battery.shell.provider;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

import io.battery.ProfileNames;

@Component
@Profile(value = ProfileNames.SHELL)
public class ShellPromptProvider implements PromptProvider {
    @Autowired
    private Environment environment;

    @Override
    public AttributedString getPrompt() {
        String profiles = String.join(",", environment.getActiveProfiles());

        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append("battery", AttributedStyle.DEFAULT
                .foreground(AttributedStyle.GREEN | AttributedStyle.BRIGHT));

        sb.append(" (", AttributedStyle.DEFAULT
                .foreground(AttributedStyle.BLUE | AttributedStyle.BRIGHT));
        sb.append(profiles, AttributedStyle.DEFAULT
                .backgroundDefault()
                .foreground(AttributedStyle.WHITE)
                .faintDefault());
        sb.append(") $ ", AttributedStyle.DEFAULT
                .foreground(AttributedStyle.BLUE | AttributedStyle.BRIGHT));

        return sb.toAttributedString();

//        String profiles = String.join(",", environment.getActiveProfiles());
//        AttributedStringBuilder sb = new AttributedStringBuilder();
//        sb.append("battery (", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
//        sb.append(profiles, AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA));
//        sb.append(")$ ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
//        return sb.toAttributedString();
    }
}
