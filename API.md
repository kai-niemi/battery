<!-- TOC -->
* [Battery REST API Guide](#battery-rest-api-guide)
  * [Introduction & Hypermedia Principles](#introduction--hypermedia-principles)
* [Tutorial & Quickstart](#tutorial--quickstart)
    * [1. Explore API Index](#1-explore-api-index)
    * [2. List Configured Scenarios](#2-list-configured-scenarios)
    * [3. Check Status](#3-check-status)
    * [4. Start a Scenario](#4-start-a-scenario)
    * [5. Monitor Workers & Errors](#5-monitor-workers--errors)
    * [6. Abort an Active Scenario](#6-abort-an-active-scenario)
* [API Reference by Resource](#api-reference-by-resource)
  * [Root Index API (`/api`)](#root-index-api-api)
  * [Scenarios API (`/api/scenario`)](#scenarios-api-apiscenario)
    * [Start Scenario Request Body Schema (`StartScenarioForm`)](#start-scenario-request-body-schema-startscenarioform)
  * [Virtual User Workers & Problems API (`/api/worker`)](#virtual-user-workers--problems-api-apiworker)
  * [Run Summaries API (`/api/run`)](#run-summaries-api-apirun)
  * [Actuators (`/api/actuator`)](#actuators-apiactuator)
* [Link Relations Reference](#link-relations-reference)
  * [IANA Link Relations](#iana-link-relations)
  * [Domain Link Relations (`battery:*`)](#domain-link-relations-battery)
* [Error Handling (RFC 7807)](#error-handling-rfc-7807)
  * [Problem Detail Properties](#problem-detail-properties)
  * [Example Error Response](#example-error-response)
* [Additional Resources](#additional-resources)
<!-- TOC -->

# Battery REST API Guide

The REST API is based
on [HAL (JSON Hypertext Application Language)](https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08)
and [Spring HATEOAS](https://docs.spring.io/spring-hateoas/docs/current/reference/html/).

## Introduction & Hypermedia Principles

The REST principle is to follow links, discover state transitions, and decide where to navigate next by consuming
hypermedia controls provided by the server. Hypermedia affordances include:

- **Link Relations (`_links`)**: Links convey available actions and transitions based on the server's current
  operational state (e.g. `start` when idle, `battery:abort` when executing).
- **CURIEs (Compact URIs)**: Domain-specific link relations are qualified under the `battery` prefix (e.g.
  `battery:scenarios`, `battery:workers`, `battery:status`), resolving to `/rels/{rel}` documentation.
- **Content Negotiation**: Endpoints support `application/hal+json` and `application/json`. Error responses use standard
  `application/problem+json`.

Clients should not hard-code dynamic URIs, but instead follow the `_links` supplied in API responses.

---

# Tutorial & Quickstart

This walkthrough demonstrates discovering capabilities, starting a test scenario, monitoring progress, and stopping
execution via `cURL`.

### 1. Explore API Index

Query the base API index to discover primary entry points:

```bash
curl -s -H "Accept: application/hal+json" http://localhost:9090/api
```

**Response:**

```json
{
  "_links" : {
    "self" : {
      "href" : "http://localhost:9090/api"
    },
    "battery:actuators" : {
      "href" : "http://localhost:9090/api/actuator",
      "title" : "Spring boot actuators"
    },
    "battery:scenarios" : {
      "href" : "http://localhost:9090/api/scenario",
      "title" : "Scenario control resource"
    },
    "battery:workers" : {
      "href" : "http://localhost:9090/api/worker{?page,size}",
      "templated" : true,
      "title" : "Virtual user workers resource"
    },
    "curies" : [ {
      "href" : "http://localhost:9090/rels/{rel}",
      "name" : "battery",
      "templated" : true
    } ]
  },
  "message" : "Battery API index resource",
  "messageType" : "information"
}
```

### 2. List Configured Scenarios

Follow `battery:scenarios` to list scenarios loaded from the effective model:

```bash
curl -s -H "Accept: application/hal+json" http://localhost:9090/api/scenario
```

**Response (partial):**

```json
{
  "_embedded" : {
    "battery:scenarios" : [ {
      "_links" : {
        "self" : {
          "href" : "http://localhost:9090/api/scenario/Customer%20CRUD/detail"
        }
      },
      "alias" : "Customer CRUD",
      "backoffOnTransientErrors" : true,
      "continueOnTransientErrors" : true,
      "duration" : "PT20S",
      "durationFormatted" : "20.00s",
      "name" : "Customer CRUD",
      "primary" : true
    } ]
  },
  "_links" : {
    "self" : {
      "href" : "http://localhost:9090/api/scenario"
    },
    "battery:status" : {
      "href" : "http://localhost:9090/api/scenario/status"
    },
    "start" : {
      "href" : "http://localhost:9090/api/scenario/start"
    },
    "curies" : [ {
      "href" : "http://localhost:9090/rels/{rel}",
      "name" : "battery",
      "templated" : true
    } ]
  }
}
```

### 3. Check Status

Check active execution state, application metadata, and model hash:

```bash
curl -s -H "Accept: application/hal+json" http://localhost:9090/api/scenario/status
```

**Response:**

```json
{
  "_links" : {
    "self" : {
      "href" : "http://localhost:9090/api/scenario/status"
    }
  },
  "appName" : "Battery",
  "appVersion" : "0.9.0-SNAPSHOT",
  "scenarioStatus" : "IDLE",
  "secureHash" : "0b30a4078fc6ecc397f0c31c2beaf957ffcb82f4bdffd9c5e923e5559978421c"
}
```

### 4. Start a Scenario

First, perform a `GET` request on `battery:scenarios` (`/api/scenario`) to retrieve the scenario resource:

```bash
curl -s -H "Accept: application/hal+json" http://localhost:9090/api/scenario
```

When idle (nothing running), the response provides the `start` link relation (`_links.start.href`). 
Follow the `start` relation by sending a `GET` request to retrieve the form template and inspect
available affordances:

```bash
curl -s -H "Accept: application/hal+json" http://localhost:9090/api/scenario/start
```

Next, send a `POST` request with the form payload back to the target URI:

```bash
curl -X POST http://localhost:9090/api/scenario/start \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer CRUD",
    "secureHash" : "0b30a4078fc6ecc397f0c31c2beaf957ffcb82f4bdffd9c5e923e5559978421c",
    "skipBeforeSteps": false,
    "skipAfterSteps": false,
    "skipPhases": false
  }'
```

*Note: If `name` is omitted or empty, a scenario is chosen according to model weights.*

**Response (HTTP 202 Accepted):**

```json
{
  "_links" : {
    "current" : {
      "href" : "http://localhost:9090/api/scenario/Customer%20CRUD/detail"
    }
  },
  "message" : "Started scenario",
  "messageType" : "information"
}
```

### 5. Monitor Workers & Errors

Query running virtual user workers and their live throughput metrics:

```bash
curl -s -H "Accept: application/hal+json" "http://localhost:9090/api/worker?page=0&size=10"
```

To inspect runtime exceptions or errors encountered during execution:

```bash
curl -s -H "Accept: application/hal+json" "http://localhost:9090/api/worker/problems?page=0&size=10"
```

### 6. Abort an Active Scenario

When a scenario is active, `GET /api/scenario` provides the `battery:abort` affordance:

```bash
curl -X POST http://localhost:9090/api/scenario/abort
```

**Response (HTTP 200 OK):**

```json
{
  "message": "Aborted",
  "messageType": "warning"
}
```

---

# API Reference by Resource

## Root Index API (`/api`)

| Method                         | Endpoint    | Status                      | Description                                                                                   |
|--------------------------------|-------------|-----------------------------|-----------------------------------------------------------------------------------------------|
| `GET`                          | `/api`      | `200 OK`                    | Root API index listing primary affordances                                                    |

## Scenarios API (`/api/scenario`)

| Method | Endpoint                      | Status                 | Description                                                                                                                      |
|--------|-------------------------------|------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `GET`  | `/api/scenario`               | `200 OK`               | Lists all configured scenarios from the model with affordances (`start` when idle, or `current` and `battery:abort` when active) |
| `GET`  | `/api/scenario/{name}/detail` | `200 OK` / `404`       | Retrieves configuration details for a named scenario                                                                             |
| `GET`  | `/api/scenario/status`        | `200 OK`               | Retrieves real-time scenario status (`idle`, `running`, `cancelling`), secure hash, and application version                      |
| `GET`  | `/api/scenario/start`         | `200 OK`               | Retrieves start scenario form template representation with default values and affordances                                        |
| `POST` | `/api/scenario/start`         | `202 Accepted` / `412` | Launches a scenario with optional parameters (`name`, `secureHash`, `skipBeforeSteps`, `skipAfterSteps`, `skipPhases`)           |
| `POST` | `/api/scenario/abort`         | `200 OK` / `412`       | Aborts the currently running scenario                                                                                            |

### Start Scenario Request Body Schema (`StartScenarioForm`)

```json
{
  "name": "scenario-name",
  "secureHash": "optional-model-secure-hash",
  "skipBeforeSteps": false,
  "skipAfterSteps": false,
  "skipPhases": false
}
```

## Virtual User Workers & Problems API (`/api/worker`)

| Method           | Endpoint                                       | Status           | Description                                                                                    |
|------------------|------------------------------------------------|------------------|------------------------------------------------------------------------------------------------|
| `GET`            | `/api/worker?page={page}&size={size}`          | `200 OK`         | Paginated list of virtual user workers (`opsPerSecond`, `p90`, `p99`, `success`, error counts) |
| `GET`            | `/api/worker/{id}`                             | `200 OK` / `404` | Retrieves details and metrics of a single worker (includes `battery:delete` link if stopped)   |
| `GET`            | `/api/worker/problems?page={page}&size={size}` | `200 OK`         | Paginated list of exceptions and errors encountered by workers                                 |
| `POST`, `DELETE` | `/api/worker/{id}/delete`                      | `204 No Content` | Removes a stopped worker by ID                                                                 |
| `POST`, `DELETE` | `/api/worker/delete`                           | `200 OK`         | Clears all non-running virtual user workers                                                    |

## Run Summaries API (`/api/run`)

Summaries of the 20 most recent scenario runs, each available once all virtual users of the
run have completed. A summary has the outcome, totals, whole-run latency percentiles in
milliseconds, users, connection pool figures, per-phase figures, grouped errors and
findings. The `latencyHistogram` is the whole-run latency histogram in microseconds as a
base64 encoded, compressed [HdrHistogram](https://hdrhistogram.github.io/HdrHistogram/), for
merging runs such as those of several agents.

| Method | Endpoint          | Status           | Description                                   |
|--------|-------------------|------------------|-----------------------------------------------|
| `GET`  | `/api/run`        | `200 OK`         | Recent run summaries, newest first            |
| `GET`  | `/api/run/latest` | `200 OK` / `404` | The most recent run summary                   |
| `GET`  | `/api/run/{id}`   | `200 OK` / `404` | A run summary by run number                   |

## Actuators (`/api/actuator`)

Spring Boot Actuator endpoints exposed under `/api/actuator` for health and agent monitoring:

- `GET /api/actuator` — Discovery index
- `GET /api/actuator/health` — Application health status
- `GET /api/actuator/info` — Application build and version information
- `POST /api/actuator/shutdown` — Application shutdown
- `GET /api/actuator/env` — Environment properties
- `GET /api/actuator/threaddump` — JVM thread dump
- `GET /api/actuator/metrics` — Metrics discovery
- `GET /api/actuator/prometheus` — Prometheus formatted metrics

---

# Link Relations Reference

## IANA Link Relations

Standard relations registered in
the [IANA Link Relations Registry](https://www.iana.org/assignments/link-relations/link-relations.xhtml):

| Link Relation | Method | Description                                                       |
|---------------|--------|-------------------------------------------------------------------|
| `self`        | `GET`  | Self reference to the enclosed resource representation            |
| `start`       | `POST` | Affordance to launch a load test scenario (`/api/scenario/start`) |
| `current`     | `GET`  | Link to the currently executing scenario details                  |
| `first`       | `GET`  | First page of items in a paginated collection                     |
| `last`        | `GET`  | Last page of items in a paginated collection                      |
| `next`        | `GET`  | Next page of items in a paginated collection                      |
| `prev`        | `GET`  | Previous page of items in a paginated collection                  |

## Domain Link Relations (`battery:*`)

Domain-specific link relations qualified under the `battery` CURIE namespace (`/rels/{rel}`):

| Link Relation            | HTTP Method      | Target / Description                                             |
|--------------------------|------------------|------------------------------------------------------------------|
| `battery:scenarios`      | `GET`            | Scenario collection resource (`/api/scenario`)                   |
| `battery:status`         | `GET`            | Current scenario runtime status (`/api/scenario/status`)         |
| `battery:abort`          | `POST`           | Abort active scenario execution (`/api/scenario/abort`)          |
| `battery:workers`        | `GET`            | Virtual user workers collection (`/api/worker`)                  |
| `battery:problems`       | `GET`            | Captured worker exceptions and problems (`/api/worker/problems`) |
| `battery:workers-delete` | `POST`, `DELETE` | Clear all non-running workers (`/api/worker/delete`)             |
| `battery:delete`         | `POST`, `DELETE` | Remove a specific stopped worker (`/api/worker/{id}/delete`)     |
| `battery:runs`           | `GET`            | Recent scenario run summaries (`/api/run`)                       |
| `battery:latest-run`     | `GET`            | Most recent scenario run summary (`/api/run/latest`)             |
| `battery:actuators`      | `GET`            | Spring Boot Actuator endpoints (`/api/actuator`)                 |

---

# Error Handling (RFC 7807)

Errors and precondition failures are returned using
standard [RFC 7807 Problem Detail](https://tools.ietf.org/html/rfc7807) with content type `application/problem+json`.

## Problem Detail Properties

- `type`: URI reference identifying the problem type
- `title`: Short, human-readable summary of the problem
- `status`: HTTP status code (e.g. `404`, `412`, `500`)
- `detail`: Human-readable explanation specific to this occurrence
- `instance`: URI reference identifying the specific occurrence

## Example Error Response

When attempting to launch a scenario while another is already running:

**HTTP Status:** `412 Precondition Failed`
**Content-Type:** `application/problem+json`

```json
{
  "type": "about:blank",
  "title": "A scenario is already running",
  "status": 412,
  "detail": "A scenario is already running",
  "instance": "/api/scenario/start"
}
```

---

# Additional Resources

- [HAL Media Type Specification](https://datatracker.ietf.org/doc/html/draft-kelly-json-hal-08)
- [Spring HATEOAS Documentation](https://docs.spring.io/spring-hateoas/docs/current/reference/html)
- [RFC 7807 Problem Details for HTTP APIs](https://tools.ietf.org/html/rfc7807)
- [IANA Link Relations Registry](https://www.iana.org/assignments/link-relations/link-relations.xhtml)
