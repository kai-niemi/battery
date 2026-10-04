package io.battery.shell;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

import tools.jackson.databind.ObjectMapper;

import io.battery.script.BatteryScript;
import io.battery.script.function.FunctionDef;
import io.battery.shell.provider.FunctionProvider;
import io.battery.shell.support.ListTableModel;
import io.battery.shell.support.ShellSupport;
import io.battery.util.DurationUtils;
import static io.battery.util.AnsiPrintWriter.wrap;

@ShellComponent
public class ScriptCommands extends AbstractShellCommand {
    @Autowired
    private BatteryScript batteryScript;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, Object> capturedState = new HashMap<>();

    @Command(value = "Execute script expression",
            name = {"execute"},
            alias = {"e"},
            group = CommandGroups.SCRIPTING_COMMANDS,
            completionProvider = "scriptFunctionProvider",
            exitStatusExceptionMapper = "commandExceptionMapper")
    public void execute(
            @Argument(index = 0, description = "Script expression to execute") String expression,
            @Option(description = "capture state and pass to next invocation", defaultValue = "false") boolean capture,
            CommandContext commandContext) {
        PrintWriter pw = wrap(commandContext.outputWriter());

        final Instant now = Instant.now();

        pw.printf("Executing [#(bright_white)%s#(default)]%n", expression);
        if (!capturedState.isEmpty()) {
            pw.printf("Passing #(bright_yellow)%d#(default) variables%n", capturedState.size());
            capturedState.forEach((k, v) ->
                    pw.printf("  %s = #(bright_yellow)%s#(default)%n", k, v));
        }

        Map<String, Object> objectMap = batteryScript.execute(expression, capturedState);

        if (capture) {
            capturedState.putAll(objectMap);
        }

        pw.println();
        if (objectMap.isEmpty()) {
            pw.println("#(bright_red)No result#(default)");
        } else {
            objectMap.forEach((k, v) -> {
                String json = objectMapper.writeValueAsString(v);
                pw.printf("#(bright_yellow)%s#(default) = #(bright_yellow)%s#(default)%n", k, json);
            });
        }

        pw.println();
        pw.printf("Time: %s total%n", DurationUtils.durationToDisplayString(Duration.between(now, Instant.now())));
        pw.flush();
    }

    @Command(description = "Show script functions grouped by namespace",
            name = {"functions"},
            alias = {"f"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            group = CommandGroups.SCRIPTING_COMMANDS)
    public void listFunctions(CommandContext commandContext) {
        PrintWriter pw = wrap(commandContext.outputWriter());

        batteryScript.forEachExternalVariable(namespace -> {
            pw.printf("#(bright_white)Namespace '%s'#(default)%n", namespace);

            List<FunctionDef> functionDefs = new ArrayList<>();

            batteryScript.forEachMethod(namespace, method -> {
                if (FunctionProvider.isQualified(method)) {
                    functionDefs.add(FunctionProvider.toFunctionDef(namespace, method));
                }
            });

            pw.println(ShellSupport.prettyPrint(new ListTableModel<>(functionDefs,
                    List.of("Signature", "Description", "Namespace", "Volatility"), (object, column) -> {
                return switch (column) {
                    case 0 -> object.getSignature();
                    case 1 -> object.getDescription();
                    case 2 -> object.getNamespace();
                    case 3 -> object.getVolatility();
                    default -> "??";
                };
            })));
        });
    }

    @Command(description = "Show captured state",
            name = {"show", "state"},
            alias = {"ss"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            group = CommandGroups.SCRIPTING_COMMANDS)
    public void showState(CommandContext commandContext) {
        PrintWriter pw = commandContext.outputWriter();
        if (!capturedState.isEmpty()) {
            capturedState.forEach((k, v) -> {
                pw.printf("%s = %s%n", k, v);
            });
        } else {
            pw.println("No state captured!");
        }
    }

    @Command(value = "Wipe captured state",
            name = {"wipe", "state"},
            alias = {"ws"},
            exitStatusExceptionMapper = "commandExceptionMapper",
            group = CommandGroups.SCRIPTING_COMMANDS)
    public void clear() {
        capturedState.clear();
    }
}
