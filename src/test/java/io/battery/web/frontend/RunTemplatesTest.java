package io.battery.web.frontend;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import io.battery.scenario.ScenarioStatus;
import io.battery.scenario.run.RunSummaries;
import io.battery.scenario.run.RunSummary;

/**
 * Renders the run templates, to catch expression errors without a running application.
 */
@Tag("unit-test")
public class RunTemplatesTest {
    private static final SpringTemplateEngine templateEngine = new SpringTemplateEngine();

    static {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        templateEngine.setTemplateResolver(resolver);
    }

    private static String render(String template, Set<String> fragments, Map<String, Object> variables) {
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse()));
        context.setVariables(variables);
        return templateEngine.process(template, fragments, context);
    }

    @Test
    public void givenRuns_expectRunsPage() {
        String html = render("run", null, Map.of("runs", List.of(
                RunSummaries.saturatedRun(2), RunSummaries.emptyCancelledRun(1))));

        Assertions.assertThat(html)
                .contains("href=\"/run/2\"", "#2", "Insert tokens", "COMPLETED", "CANCELLED", "text-bg-danger");
    }

    @Test
    public void givenNoRuns_expectEmptyRunsPage() {
        Assertions.assertThat(render("run", null, Map.of("runs", List.of())))
                .contains("No finished runs yet");
    }

    @Test
    public void givenSaturatedRun_expectRunDetailPage() {
        RunSummary run = RunSummaries.saturatedRun(2);

        String html = render("run-detail", null, Map.of("run", run));

        Assertions.assertThat(html)
                .contains("Run #2 · Insert tokens", "Warm up phase", "Peak phase", "after phases",
                        "Connection pool saturated", "40 users dropped", "IllegalStateException",
                        "serialization failure", "href=\"/api/run/2\"")
                .doesNotContain("${");
    }

    @Test
    public void givenEmptyRun_expectRunDetailPageWithoutPool() {
        String html = render("run-detail", null, Map.of("run", RunSummaries.emptyCancelledRun(1)));

        Assertions.assertThat(html)
                .contains("Run #1", "CANCELLED", "by operator")
                .doesNotContain("Pool size");
    }

    @Test
    public void givenLastRun_expectRunCardInStatusPanel() {
        String html = render("scenario", Set.of("statusPanel"), Map.of(
                "activeStatus", ScenarioStatus.COMPLETED,
                "activeTitle", "No active scenario",
                "lastRun", RunSummaries.saturatedRun(3)));

        Assertions.assertThat(html)
                .contains("Run #3", "href=\"/run/3\"", "Findings", "users failed");
    }
}
