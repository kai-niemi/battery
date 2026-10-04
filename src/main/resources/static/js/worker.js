const WorkerDashboard = function (settings) {
    this.settings = settings;
    this.init();
};

WorkerDashboard.prototype = {
    init: function () {
        var socket = new SockJS(this.settings.endpoints.socket),
                stompClient = Stomp.over(socket),
                _this = this;
        // stompClient.log = (log) => {};
        stompClient.connect({}, function (frame) {
            stompClient.subscribe(_this.settings.topics.refresh, function () {
                _this.handleRefresh();
            });

            stompClient.subscribe(_this.settings.topics.update, function () {
                _this.handleModelUpdate();
            });
        });
    },

    getElement: function (id) {
        return $('#' + id);
    },

    round: function (v) {
        return (v !== undefined && v !== null) ? Number(v).toFixed(1) : "0.0";
    },

    loadTableRows: function () {
        var queryString = window.location.search;
        var url = '/worker/table-rows' + queryString;
        var $tbody = $('#workers-tbody');

        if ($tbody.length === 0) {
            location.reload();
            return;
        }

        $.get(url, function (html) {
            var $newContent = $(html);
            if ($newContent.is('#workers-tbody')) {
                $('#workers-tbody').replaceWith($newContent);
            } else {
                $('#workers-tbody').html(html);
            }
        });
    },

    loadPagingBanner: function () {
        var queryString = window.location.search;
        var url = '/worker/paging-banner' + queryString;
        var $pagingContainer = $('#paging-banner-container');

        if ($pagingContainer.length === 0) {
            return;
        }

        $.get(url, function (html) {
            var $newContent = $(html);
            if ($newContent.is('#paging-banner-container')) {
                $('#paging-banner-container').replaceWith($newContent);
            } else {
                $('#paging-banner-container').html(html);
            }
        });
    },

    handleRefresh: function () {
        this.handleModelUpdate();
    },

    handleModelUpdate: function () {
        var _this = this;
        const queryString = window.location.search;

        _this.loadTableRows();
        _this.loadPagingBanner();

        $.getJSON("/worker/summary" + queryString, function(json) {
            _this.handleWorkerSummaryUpdate(json);
        });
    },

    handleWorkerSummaryUpdate: function (metrics) {
        if (!metrics) return;
        var _this = this;

        const metricElt = _this.getElement("aggregated-metrics");
        metricElt.find(".p90").text(_this.round(metrics.p90));
        metricElt.find(".p99").text(_this.round(metrics.p99));
        metricElt.find(".p999").text(_this.round(metrics.p999));
        metricElt.find(".opsPerSec").text(_this.round(metrics.opsPerSec));
        metricElt.find(".opsPerMin").text(_this.round(metrics.opsPerMin));
        metricElt.find(".success").text(metrics.success ?? 0);
        metricElt.find(".transientFail").text(metrics.transientFail ?? 0);
        metricElt.find(".nonTransientFail").text(metrics.nonTransientFail ?? 0);
    },
};

document.addEventListener('DOMContentLoaded', function () {
    new WorkerDashboard({
        endpoints: {
            socket: '/battery-service',
        },

        topics: {
            update: '/topic/worker/update',
            refresh: '/topic/worker/refresh',
        },
    });
});

