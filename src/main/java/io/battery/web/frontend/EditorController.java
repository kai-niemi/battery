package io.battery.web.frontend;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import tools.jackson.databind.ObjectMapper;

import io.battery.ProfileNames;
import io.battery.metrics.Problem;
import io.battery.model.ScriptFormat;
import io.battery.model.Step;
import io.battery.scenario.step.StepActionProvider;
import io.battery.script.BatteryScript;
import io.battery.script.BatteryScriptException;
import io.battery.util.ByteFormat;
import io.battery.util.DurationUtils;
import io.battery.web.frontend.model.ScriptForm;

@WebController
@Profile(value = ProfileNames.ONLINE)
@RequestMapping("/playground")
@SessionAttributes("capturedOutput")
public class EditorController {
    @Autowired
    private BatteryScript batteryScript;

    @Autowired
    private StepActionProvider stepActionProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @ModelAttribute("capturedOutput")
    public Map<String, Object> capturedOutputModel() {
        return new HashMap<>();
    }

    @ModelAttribute("form")
    public ScriptForm scriptFormModel() {
        ScriptForm form = new ScriptForm();
        form.setScriptFormat(ScriptFormat.BATTERY);
        form.setInput("""
                a="The quick brown fox jumps over the lazy dog";
                b="Today is %s at %s".formatted(std.currentDate(), std.currentTime());
                c=2^4+1+2*(3-5);
                d=c+10;
                e=java.lang.Math.PI ^ 2;
                f=java.lang.Math.sin(90) + 1.0;
                """);
        return form;
    }

    @GetMapping
    public Callable<String> indexPage() {
        return () -> "playground";
    }

    @PostMapping(params = "action=validate")
    public Callable<String> validateScript(@ModelAttribute("form") @Valid ScriptForm form,
                                           Model model) {
        return () -> {
            try {
                if (form.getScriptFormat().equals(ScriptFormat.BATTERY)) {
                    batteryScript.validate(form.getInput());
                    form.setOutput("Validation passed!");
                } else {
                    form.setOutput("Validating format "
                                   + form.getScriptFormat() + " is unsupported!");
                }
            } catch (BatteryScriptException e) {
                form.setOutput(e.getMessage());
                form.setErrorFromLine(e.getOffendingTokenOffset().getFirst());
                form.setErrorFromChar(e.getOffendingTokenOffset().getSecond());
            }

            model.addAttribute("form", form);

            return "playground";
        };
    }

    @PostMapping(params = "action=execute")
    public Callable<String> executeScript(@ModelAttribute("form") @Valid ScriptForm form,
                                          @SessionAttribute(value = "capturedOutput")
                                          Map<String, Object> capturedOutput,
                                          Model model) {
        return () -> {
            final Instant now = Instant.now();
            final List<String> lines = new ArrayList<>();
            final Map<String, Object> outputFiltered = new LinkedHashMap<>();

            try {
                final Step step = new Step();
                step.setName("Playground");
                if (form.getScriptFormat().equals(ScriptFormat.BATTERY)) {
                    step.setScript(form.getInput());
                } else {
                    step.setSql(form.getInput());
                }
                step.setCapture(form.getCapture());

                stepActionProvider.evictCache();

                final Map<String, Object> output = stepActionProvider.findAction(step)
                        .perform(step, Collections.unmodifiableMap(capturedOutput));

                if (form.getCapture()) {
                    capturedOutput.clear();
                    capturedOutput.putAll(output);
                }

                final AtomicLong totalSize = new AtomicLong();

                output.forEach((k, v) -> {
                    String data = objectMapper.writeValueAsString(v);

                    if (totalSize.addAndGet(data.length()) > ByteFormat.ONE_MB * 2) {
                        outputFiltered.put(k, "(too much data... %s)"
                                .formatted(ByteFormat.byteCountToDisplaySize(totalSize.get())));
                    } else {
                        if (data.length() < ByteFormat.ONE_KB * 256) {
                            outputFiltered.put(k, v);
                        } else {
                            outputFiltered.put(k, "(value too large: %s)"
                                    .formatted(ByteFormat.byteCountToDisplaySize(data.getBytes().length)));
                        }
                    }
                });

                lines.add(objectMapper.writeValueAsString(outputFiltered));

                lines.add("Time: "
                          + DurationUtils.durationToDisplayString(Duration.between(now, Instant.now()))
                          + " total");

                form.setOutput(String.join("\n", lines));
            } catch (BatteryScriptException e) {
                Problem problem = Problem.of(e);
                form.setOutput(problem.getStackTrace());
                form.setErrorFromLine(e.getOffendingTokenOffset().getFirst());
                form.setErrorFromChar(e.getOffendingTokenOffset().getSecond());
            } catch (Exception e) {
                Problem problem = Problem.of(e);
                form.setOutput(problem.getStackTrace());
            }

            model.addAttribute("form", form);

            return "playground";
        };
    }

    @PostMapping(params = "action=reset")
    public Callable<String> resetScript(@ModelAttribute("form") ScriptForm form,
                                        HttpSession session,
                                        SessionStatus status) {
        return () -> {
            status.setComplete();
            session.removeAttribute("capturedOutput");
            return "redirect:/playground";
        };
    }
}
