var backgroundColors = [
    'rgba(255, 99, 132, 0.2)',
    'rgba(255, 159, 64, 0.2)',
    'rgba(255, 205, 86, 0.2)',
    'rgba(75, 192, 192, 0.2)',
    'rgba(54, 162, 235, 0.2)',
    'rgba(153, 102, 255, 0.2)',
    'rgba(201, 203, 207, 0.2)'
];


var borderColors = [
    'rgb(255, 99, 132)',
    'rgb(255, 159, 64)',
    'rgb(255, 205, 86)',
    'rgb(75, 192, 192)',
    'rgb(54, 162, 235)',
    'rgb(153, 102, 255)',
    'rgb(201, 203, 207)'
];

const chartWorkers = new Chart(document.getElementById("chart-container-stats"), {
    type: 'line',
    data: {
        labels: [],
        datasets: [],
    },
    options: {
        scales: {
            x: {
                type: 'time',
                time: {
                    unit: 'minute'
                },
                parse: false
            },
            y: {
                title: {
                    display: true,
                    text: "Value",
                },
            },
        },
        plugins: {
            title: {
                display: true,
                text: 'Virtual Users (gauges)'
            },
        },
        responsive: true,
    },
});

const chartP99 = new Chart(document.getElementById("chart-container-p99"), {
    type: 'line',
    data: {
        labels: [],
        datasets: [],
    },
    options: {
        scales: {
            x: {
                type: 'time',
                time: {
                    unit: 'minute'
                },
                parse: false
            },
            y: {
                title: {
                    display: true,
                    text: "P99 Latency (ms)",
                },
            },
        },
        plugins: {
            title: {
                display: true,
                text: 'P99 Latency (ms)'
            },
        },
        responsive: true,
    },
});

const chartP999 = new Chart(document.getElementById("chart-container-p999"), {
    type: 'line',
    data: {
        labels: [],
        datasets: [],
    },
    options: {
        scales: {
            x: {
                type: 'time',
                time: {
                    unit: 'minute'
                },
                parse: false
            },
            y: {
                title: {
                    display: true,
                    text: "P99.9 Latency (ms)",
                },
            },
        },
        plugins: {
            title: {
                display: true,
                text: 'P99.9 Latency (ms)'
            },
        },
        responsive: true,
    },
});

const chartTPS = new Chart(document.getElementById("chart-container-tps"), {
    type: 'line',
    data: {
        labels: [],
        datasets: [],
    },
    options: {
        scales: {
            x: {
                type: 'time',
                time: {
                    unit: 'minute'
                },
                parse: false
            },
            y: {
                title: {
                    display: true,
                    text: "Transactions per second (TpS)",
                },
            },
        },
        plugins: {
            title: {
                display: true,
                text: 'Transactions per second (TpS)'
            },
        },
        responsive: true,
    },
});

const WorkerChartsDashboard = function (settings) {
    this.settings = settings;
    this.init();
};

WorkerChartsDashboard.prototype = {
    init: function () {
        var socket = new SockJS(this.settings.endpoints.socket),
                stompClient = Stomp.over(socket),
                _this = this;
        // stompClient.log = (log) => {};
        stompClient.connect({}, function (frame) {
            stompClient.subscribe(_this.settings.topics.charts, function () {
                _this.handleChartsUpdate();
            });
        });
    },

    updateChart: function (chart,json) {
        const xValues = json[0]["data"];

        const yValues = json.filter((item, idx) => idx > 0)
                .map(function(item) {
                    var id = item["id"];
                    var bgColor = backgroundColors[id % backgroundColors.length];
                    var ogColor = borderColors[id % borderColors.length];
                    return {
                        label: item["name"],
                        data: item["data"],
                        backgroundColor: bgColor,
                        borderColor: ogColor,
                        fill: false,
                        tension: 1.2,
                        cubicInterpolationMode: 'monotone',
                        borderWidth: 1,
                        hoverOffset: 4,
                    };
                });

        const visibleStates=[];
        chart.data.datasets.forEach((dataset, datasetIndex) => {
            visibleStates.push(chart.isDatasetVisible(datasetIndex));
        });

        chart.config.data.labels = xValues;
        chart.config.data.datasets = yValues;

        if (visibleStates.length > 0) {
            chart.data.datasets.forEach((dataset, datasetIndex) => {
                chart.setDatasetVisibility(datasetIndex, visibleStates[datasetIndex]);
            });
        }

        chart.update('none');
    },

    handleChartsUpdate: function() {
        var _this = this;

        const queryString = window.location.search;

        $.getJSON("api/chart/workers" + queryString, function(json) {
            _this.updateChart(chartWorkers,json);
        });

        $.getJSON("api/chart/workers/p99" + queryString, function(json) {
            _this.updateChart(chartP99,json);
        });

        $.getJSON("api/chart/workers/p999" + queryString, function(json) {
            _this.updateChart(chartP999,json);
        });

        $.getJSON("api/chart/workers/tps" + queryString, function(json) {
            _this.updateChart(chartTPS,json);
        });
    },
};

document.addEventListener('DOMContentLoaded', function () {
    new WorkerChartsDashboard({
        endpoints: {
            socket: '/battery-service',
        },

        topics: {
            charts: '/topic/worker/charts',
        },
    });
});

