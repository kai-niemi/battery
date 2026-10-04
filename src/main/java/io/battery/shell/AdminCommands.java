package io.battery.shell;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.shell.core.command.CommandContext;
import org.springframework.shell.core.command.annotation.Command;

import io.battery.shell.support.ExceptionEvent;
import io.battery.shell.support.GenericEvent;
import io.battery.util.DurationUtils;
import static io.battery.util.AnsiPrintWriter.wrap;

@ShellComponent
public class AdminCommands extends AbstractShellCommand {
    @Autowired
    private ConfigurableApplicationContext applicationContext;

    private Throwable lastException;

    @EventListener
    public void onExceptionEvent(GenericEvent<ExceptionEvent> event) {
        this.lastException = event.getTarget().getThrowable();
    }

    @Command(value = "Exit the shell",
            name = {"quit"},
            alias = {"q"},
            group = CommandGroups.ADMIN_COMMANDS)
    public void quit(CommandContext ctx) {
        ctx.outputWriter().println("Quitting");
        SpringApplication.exit(applicationContext, () -> 0);
        System.exit(0);
    }

    @Command(description = "Show last exception stacktrace",
            name = {"stacktrace"},
            alias = "x",
            group = CommandGroups.ADMIN_COMMANDS)
    public void printLastException(CommandContext commandContext) {
        if (lastException != null) {
            lastException.printStackTrace(commandContext.outputWriter());
        } else {
            commandContext.outputWriter().println("No exception registered!");
        }
    }

    @Command(value = "Show application uptime",
            name = {"uptime"},
            group = CommandGroups.ADMIN_COMMANDS)
    public void uptime(CommandContext ctx) {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        wrap(ctx.outputWriter()).printf(
                "#(bright_white)Uptime is: #bg(red)#(bright_yellow)%s#(default)#bg(default)%n",
                DurationUtils.durationToDisplayString(Duration.ofMillis(uptime)));
    }

    @Command(value = "Show system information",
            name = {"system"},
            alias = {"y"},
            group = CommandGroups.ADMIN_COMMANDS)
    public void systemInfo(CommandContext ctx) {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

        PrintWriter pw = wrap(ctx.outputWriter());
        pw.println("#(bright_white)>> OS#(default)");
        pw.println(" Arch: %s | OS: %s | Version: %s".formatted(os.getArch(), os.getName(), os.getVersion()));
        pw.println(" Available processors: %d".formatted(os.getAvailableProcessors()));
        pw.println(" Load avg: %f".formatted(os.getSystemLoadAverage()));

        RuntimeMXBean r = ManagementFactory.getRuntimeMXBean();
        pw.println("#(bright_white)>> Runtime#(default)");
        pw.println(" Uptime: %s".formatted(r.getUptime()));
        pw.println(" VM name: %s | Vendor: %s | Version: %s".formatted(r.getVmName(), r.getVmVendor(), r.getVmVersion()));

        ThreadMXBean t = ManagementFactory.getThreadMXBean();
        pw.println("#(bright_white)>> Threads#(default)");
        pw.println(" CPU time: %d".formatted(t.getCurrentThreadCpuTime()));
        pw.println(" User time: %d".formatted(t.getCurrentThreadUserTime()));
        pw.println(" Peak threads: %d".formatted(t.getPeakThreadCount()));
        pw.println(" Thread #: %d".formatted(t.getThreadCount()));
        pw.println(" Total started threads: %d".formatted(t.getTotalStartedThreadCount()));

        Arrays.stream(t.getAllThreadIds()).sequential().forEach(value -> {
            pw.println(" Thread (%d): %s %s".formatted(value,
                    t.getThreadInfo(value).getThreadName(),
                    t.getThreadInfo(value).getThreadState().toString()
            ));
        });

        MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        pw.println("#(bright_white)>> Memory#(default)");
        pw.println(" Heap: %s".formatted(m.getHeapMemoryUsage().toString()));
        pw.println(" Non-heap: %s".formatted(m.getNonHeapMemoryUsage().toString()));
    }
}
