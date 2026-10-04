$('document').ready(function () {
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'))
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl)
    })

    CodeMirror.fromTextArea(document.getElementById('modelEditor'), {
        lineNumbers: true,
        lineWrapping: true,
        readOnly: true,
        mode: 'text/x-perl',
        theme: 'abbott'
    });
});

const ScenarioDashboard = function (settings) {
    this.settings = settings;
    this.init();
};

ScenarioDashboard.prototype = {
    init: function () {
        var socket = new SockJS(this.settings.endpoints.socket),
            stompClient = Stomp.over(socket),
            _this = this;

        // stompClient.log = (log) => {};

        stompClient.connect({}, function (frame) {
            stompClient.subscribe(_this.settings.topics.refresh, function () {
                _this.handleRefresh();
            });

            stompClient.subscribe(_this.settings.topics.progress, function (payload) {
                var _event = JSON.parse(payload.body);
                _this.handlePhaseProgressEvent(_event);
            });
        });
    },

    loadStatusPanel: function () {
        var $statusContainer = $('#scenario-status-container');
        if ($statusContainer.length === 0) {
            location.reload();
            return;
        }

        $.get('/scenario/status-panel', function (html) {
            var $newContent = $(html);
            if ($newContent.is('#scenario-status-container')) {
                $('#scenario-status-container').replaceWith($newContent);
            } else {
                $('#scenario-status-container').html(html);
            }
        });
    },

    handleRefresh: function () {
        this.loadStatusPanel();
    },

    getElement: function (id) {
        return $('#' + id);
    },

    round: function (v) {
        return v.toFixed(0);
    },

    handlePhaseProgressEvent: function (event) {
        var _this = this;

        var _divElt = _this.getElement(event.tag);
        var _spinnerElt = _divElt.find(".progress-spinner");
        var _progressBarElt = _divElt.find(".progress-bar");

        if (event.progress >= 100) {
            _spinnerElt.attr("style", "display: none");
        } else {
            _spinnerElt.attr("style", "display: block");
        }

        _progressBarElt.attr("style", "width: " + _this.round(event.progress) + "%");
        _progressBarElt.text(_this.round(event.progress) + "%");

        console.log("Progressbar: " + _progressBarElt);
    },
};

document.addEventListener('DOMContentLoaded', function () {
    new ScenarioDashboard({
        endpoints: {
            socket: '/battery-service',
        },

        topics: {
            progress: '/topic/scenario/phase/progress',
            refresh: '/topic/scenario/refresh',
        },
    });
});

