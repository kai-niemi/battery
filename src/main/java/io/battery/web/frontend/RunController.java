package io.battery.web.frontend;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import io.battery.ProfileNames;
import io.battery.scenario.run.RunRecorder;

/**
 * Pages for the most recent scenario runs and the summary of a run, with its key metrics,
 * findings, phases and errors.
 */
@WebController
@Profile(value = ProfileNames.ONLINE)
@RequestMapping("/run")
public class RunController {
    @Autowired
    private RunRecorder runRecorder;

    @GetMapping
    public Callable<String> runsPage(Model model) {
        return () -> {
            model.addAttribute("runs", runRecorder.getRecentRuns());
            return "run";
        };
    }

    @GetMapping("/{id}")
    public Callable<String> runPage(@PathVariable("id") Integer id, Model model) {
        return () -> {
            model.addAttribute("run", runRecorder.getRun(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No such recent run: #" + id)));
            return "run-detail";
        };
    }
}
