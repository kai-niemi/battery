package io.battery.web.frontend;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import io.battery.metrics.Metrics;
import io.battery.scenario.worker.WorkTracker;
import io.battery.scenario.worker.Worker;
import io.battery.scenario.worker.WorkerStatus;
import io.battery.web.frontend.model.FilterForm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit-test")
public class WorkerControllerTest {
    private WorkTracker workTracker;

    private WorkerController workerController;

    @BeforeEach
    public void setUp() {
        workTracker = mock(WorkTracker.class);
        SimpMessagePublisher simpMessagePublisher = mock(SimpMessagePublisher.class);

        workerController = new WorkerController();
        ReflectionTestUtils.setField(workerController, "workTracker", workTracker);
        ReflectionTestUtils.setField(workerController, "simpMessagePublisher", simpMessagePublisher);
    }

    @Test
    public void testIndexPage() throws Exception {
        Page<Worker> emptyPage = new PageImpl<>(Collections.emptyList());
        when(workTracker.listWorkers(any(Pageable.class), any())).thenReturn(emptyPage);
        when(workTracker.getMetricsAggregate(any(Pageable.class))).thenReturn(mock(Metrics.class));

        Model model = new ConcurrentModel();
        String viewName = workerController.indexPage(null, PageRequest.of(0, 15), model).call();

        assertThat(viewName).isEqualTo("worker");
        assertThat(model.getAttribute("form")).isInstanceOf(FilterForm.class);
        assertThat(model.getAttribute("workerPage")).isEqualTo(emptyPage);
        assertThat(model.getAttribute("aggregatedMetrics")).isNotNull();
    }

    @Test
    public void testTableRowsFragment() throws Exception {
        Page<Worker> emptyPage = new PageImpl<>(Collections.emptyList());
        when(workTracker.listWorkers(any(Pageable.class), any())).thenReturn(emptyPage);
        when(workTracker.getMetricsAggregate(any(Pageable.class))).thenReturn(mock(Metrics.class));

        Model model = new ConcurrentModel();
        String viewName = workerController.tableRows(WorkerStatus.RUNNING, PageRequest.of(0, 15), model).call();

        assertThat(viewName).isEqualTo("worker :: workerRows");
        assertThat(model.getAttribute("form")).isInstanceOf(FilterForm.class);
        FilterForm form = (FilterForm) model.getAttribute("form");
        assertThat(form.getStatus()).isEqualTo(WorkerStatus.RUNNING);
        assertThat(model.getAttribute("workerPage")).isEqualTo(emptyPage);
        assertThat(model.getAttribute("aggregatedMetrics")).isNotNull();
    }

    @Test
    public void testPagingBannerFragment() throws Exception {
        Page<Worker> emptyPage = new PageImpl<>(Collections.emptyList());
        when(workTracker.listWorkers(any(Pageable.class), any())).thenReturn(emptyPage);
        when(workTracker.getMetricsAggregate(any(Pageable.class))).thenReturn(mock(Metrics.class));

        Model model = new ConcurrentModel();
        String viewName = workerController.pagingBanner(WorkerStatus.RUNNING, PageRequest.of(0, 15), model).call();

        assertThat(viewName).isEqualTo("worker :: pagingBanner");
        assertThat(model.getAttribute("form")).isInstanceOf(FilterForm.class);
        FilterForm form = (FilterForm) model.getAttribute("form");
        assertThat(form.getStatus()).isEqualTo(WorkerStatus.RUNNING);
        assertThat(model.getAttribute("workerPage")).isEqualTo(emptyPage);
        assertThat(model.getAttribute("aggregatedMetrics")).isNotNull();
    }
}
