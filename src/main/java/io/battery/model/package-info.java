/**
 * The workload model that Battery binds from its {@code battery} configuration properties:
 * <pre>
 * BatteryModel
 * ├── before: Before          steps run once before the phases
 * ├── phases: [Phase]         periods that create virtual users (VUs)
 * ├── scenarios: [Scenario]   steps that each VU runs repeatedly
 * │   └── steps: [Step]
 * ├── after: After            steps run once after the phases
 * ├── network: Network        remote agents to control
 * │   └── agents: [Agent]
 * └── connectionPool: ConnectionPool   pool sizing for launches
 * </pre>
 */
package io.battery.model;
