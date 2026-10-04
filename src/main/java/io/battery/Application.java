package io.battery;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.util.StringUtils;

import io.battery.model.BatteryModel;
import io.battery.util.PathUtils;
import static io.battery.util.AnsiPrintWriter.wrap;

@EnableConfigurationProperties(value = {BatteryModel.class})
@SpringBootApplication(exclude = {
        TransactionAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        DataJdbcRepositoriesAutoConfiguration.class,
})
public class Application {
    public static final String SPRING_SHELL_INTERACTIVE_ENABLED = "spring.shell.interactive.enabled";

    public static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

    private static void printHelpAndExit(Consumer<PrintWriter> message) {
        PrintWriter pw = wrap(new PrintWriter(new OutputStreamWriter(System.out)));
        pw.printf("#(by)Usage: java -jar battery.jar [options] <profile> [args...]#(d)%n%n");
        pw.printf("Undefined options are passed through to spring boot.%n%n");
        pw.printf("#(bc)Options include:#(d)%n");
        pw.printf("--help                    this help%n");
        pw.printf("--noshell                 disable interactive shell%n");
        pw.printf("--offline                 dont start embedded Jetty container (shell only)%n");
        pw.printf("--profiles [profile,..]   override spring profiles to activate%n");
        pw.printf("@<file>                   read shell commands from file (non-interactive)%n");
        pw.println();

        message.accept(pw);

        pw.println();

        System.exit(0);
    }

    public static void main(String[] args) throws IOException {
        LinkedList<String> argsList = new LinkedList<>(Arrays.asList(args));
        LinkedList<String> passThroughArgs = new LinkedList<>();
        WebApplicationType webApplicationType = WebApplicationType.SERVLET;

        Set<String> profiles =
                StringUtils.commaDelimitedListToSet(System.getProperty(SPRING_PROFILES_ACTIVE));

        // Profiles denoting a run mode rather than a load test configuration. Kept apart
        // so that a later --profiles option doesn't clear them out, which makes the
        // option order of --noshell/--offline and --profiles insignificant.
        Set<String> runModeProfiles = new LinkedHashSet<>();

        while (!argsList.isEmpty()) {
            String arg = argsList.pop();
            if (arg.equals("--help")) {
                printHelpAndExit(pw -> {
                });
            } else if (arg.equals("--profiles")) {
                if (argsList.isEmpty()) {
                    printHelpAndExit(pw ->
                            pw.println("#(bright_red)Expected list of profile names#(default)"));
                }
                profiles.clear();
                profiles.addAll(StringUtils.commaDelimitedListToSet(argsList.pop()));
            } else if (arg.equals("--offline")) {
                runModeProfiles.add(ProfileNames.OFFLINE);
                webApplicationType = WebApplicationType.NONE;
            } else if (arg.equals("--noshell")) {
                runModeProfiles.add(ProfileNames.NOSHELL);
                System.setProperty(SPRING_SHELL_INTERACTIVE_ENABLED, "false");
            } else {
                if (arg.startsWith("--") || arg.startsWith("@")) {
                    passThroughArgs.add(arg);
//                    System.setProperty(SPRING_SHELL_INTERACTIVE_ENABLED, "false");
                } else {
                    profiles.add(arg);
                }
            }
        }

        if (profiles.isEmpty()) {
            List<Path> files = PathUtils.listApplicationYamlFiles(Path.of("config"));
            if (files.isEmpty()) {
                throw new IOException(
                        "Unable to find any 'application-<profile>.yml' file in 'config/'");
            }

            printHelpAndExit(
                    pw -> {
                        String found = String.join(", ", PathUtils.toProfileNames(files));
                        pw.printf("#(br)%s#(d)%n",
                                "Missing profiles but found candidates: " + found);
                    });
        }

        profiles.addAll(runModeProfiles);

        System.setProperty(SPRING_PROFILES_ACTIVE, String.join(",", profiles));

        new SpringApplicationBuilder(Application.class)
                .web(webApplicationType)
                .logStartupInfo(true)
                .profiles(profiles.toArray(new String[0]))
                .run(passThroughArgs.toArray(new String[] {}));
    }
}
