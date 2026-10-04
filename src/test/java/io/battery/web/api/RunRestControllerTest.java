package io.battery.web.api;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.battery.scenario.run.RunRecorder;
import io.battery.scenario.run.RunSummaries;
import io.battery.scenario.run.RunSummary;
import io.battery.web.api.model.LinkRelations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit-test")
public class RunRestControllerTest {
    private final RunSummary run = RunSummaries.saturatedRun(2);

    private RunRecorder runRecorder;

    private RunRestController controller;

    @BeforeEach
    public void setUp() {
        // Links are built from the current request
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        runRecorder = mock(RunRecorder.class);
        controller = new RunRestController();
        ReflectionTestUtils.setField(controller, "runRecorder", runRecorder);
    }

    @AfterEach
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void givenRuns_expectRunsWithLinks() {
        when(runRecorder.getRecentRuns()).thenReturn(List.of(run));

        ResponseEntity<CollectionModel<EntityModel<RunSummary>>> response
                = (ResponseEntity<CollectionModel<EntityModel<RunSummary>>>) controller.findRuns();

        CollectionModel<EntityModel<RunSummary>> body = response.getBody();
        Assertions.assertThat(body.getContent())
                .singleElement()
                .satisfies(model -> {
                    Assertions.assertThat(model.getContent()).isEqualTo(run);
                    Assertions.assertThat(model.getRequiredLink(IanaLinkRelations.SELF).getHref())
                            .endsWith("/api/run/2");
                });
        Assertions.assertThat(body.getRequiredLink(LinkRelations.LATEST_RUN_REL).getHref())
                .endsWith("/api/run/latest");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void givenRun_expectRunById() {
        when(runRecorder.getRun(2)).thenReturn(Optional.of(run));

        ResponseEntity<EntityModel<RunSummary>> response
                = (ResponseEntity<EntityModel<RunSummary>>) controller.findRun(2);

        Assertions.assertThat(response.getBody().getContent()).isEqualTo(run);
        Assertions.assertThat(response.getBody().getRequiredLink(LinkRelations.RUNS_REL).getHref())
                .endsWith("/api/run");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void givenNoRun_expectNotFound() {
        when(runRecorder.getRun(9)).thenReturn(Optional.empty());
        when(runRecorder.getLastRun()).thenReturn(Optional.empty());

        Assertions.assertThat(((ResponseEntity<?>) controller.findRun(9)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        Assertions.assertThat(((ResponseEntity<?>) controller.findLatestRun()).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
