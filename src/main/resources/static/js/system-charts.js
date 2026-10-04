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

const chartThreadPool = new Chart(document.getElementById("chart-container-thread-pool"), {
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
                    text: "Metric (gauge)",
                },
            },
        },
        plugins: {
            title: {
                display: true,
                text: 'Virtual Thread Stats'
            },
        },
        responsive: true,
    },
});

const chartConnectionPool = new Chart(document.getElementById("chart-container-connection-pool"), {
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
                    text: "Metric (gauge)",
                },
            },
        },
        plugins: {
            title: {
                display: true,
                text: 'Hikari Connection Pool Stats'
            },
        },
        responsive: true,
    },
});

const chartConnectionPoolTimings = new Chart(document.getElementById("chart-container-connection-pool-timings"), {
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
                    text: "Time (ms)",
                },
            },
        },
        plugins: {
            title: {
                display: true,
                text: 'Hikari Connection Pool Timings'
            },
        },
        responsive: true,
    },
});

const chartCpu = new Chart(document.getElementById("chart-container-cpu"), {
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
                    text: "Utilization (gauge)",
                },
            },
        },
        plugins: {
            title: {
                display: true,
                text: 'CPU'
            },
        },
        responsive: true,
    },
});

const SystemChartsDashboard = function (settings) {
    this.settings = settings;
    this.init();
};

SystemChartsDashboard.prototype = {
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
                .map(function(item, idx) {
                    // Ids are meter names rather than numbers, so pick colors by position
                    var bgColor = backgroundColors[idx % backgroundColors.length];
                    var ogColor = borderColors[idx % borderColors.length];
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

        $.getJSON("api/chart/thread-pool", function (json) {
            _this.updateChart(chartThreadPool,json);
        });

        $.getJSON("api/chart/connection-pool", function(json) {
            _this.updateChart(chartConnectionPool,json);
        });

        $.getJSON("api/chart/connection-pool-timings", function(json) {
            _this.updateChart(chartConnectionPoolTimings,json);
        });

        $.getJSON("api/chart/cpu", function(json) {
            _this.updateChart(chartCpu,json);
        });
    },
};

document.addEventListener('DOMContentLoaded', function () {
    new SystemChartsDashboard({
        endpoints: {
            socket: '/battery-service',
        },

        topics: {
            charts: '/topic/system/charts',
        },
    });
});

