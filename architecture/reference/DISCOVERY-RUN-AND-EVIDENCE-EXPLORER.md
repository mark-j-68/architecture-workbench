# Discovery Run and Evidence Explorer

## Purpose

Release 0.2.6 makes deterministic discovery output inspectable before architectural interpretation. A workspace user can run local repository discovery, inspect every plugin execution, browse evidence and neutral observations, review structural metrics and diagnostics, and follow supporting-evidence links back to source provenance.

The exposed Release 0.2 flow is:

```text
Repository -> Plugin Execution -> Evidence -> Deterministic Observation -> Structural Metric
```

Product-level findings, risks, scores, and recommendations remain Release 0.3 responsibilities.

## Lifecycle

A synchronous discovery run has a stable run id, workspace id, source reference, start/completion timestamps, correlation id, initiating causation id, status, and summary counts. The read model supports `REQUESTED`, `RUNNING`, `COMPLETED`, `PARTIALLY_COMPLETED`, and `FAILED`; synchronous API calls normally return a terminal state.

One failed plugin does not cancel subsequent plugins. Successful output remains available. Any failed or partial plugin makes the run partially completed unless every plugin failed. Typed `DiscoveryStarted` and `DiscoveryCompleted` audit events share the run correlation id and retain separate causation metadata. No event is emitted for every evidence item.

## API read model

The API exposes immutable DTOs rather than discovery domain objects:

- `DiscoveryRunSummary` and `DiscoveryRunDetails`;
- `DiscoveryPluginExecution`;
- `DiscoveryEvidenceView` and `DiscoveryProvenanceView`;
- `DiscoveryObservationView`;
- `DiscoveryMetricView`;
- `DiscoveryDiagnosticView`;
- `DiscoveryConfidenceView`.

Endpoints:

```text
POST /api/workspaces/{workspaceId}/discovery-runs
GET  /api/workspaces/{workspaceId}/discovery-runs
GET  /api/workspaces/{workspaceId}/discovery-runs/{runId}
GET  /api/workspaces/{workspaceId}/discovery-runs/{runId}/plugins
GET  /api/workspaces/{workspaceId}/discovery-runs/{runId}/evidence
GET  /api/workspaces/{workspaceId}/discovery-runs/{runId}/observations
GET  /api/workspaces/{workspaceId}/discovery-runs/{runId}/metrics
GET  /api/workspaces/{workspaceId}/discovery-runs/{runId}/diagnostics
```

The existing `POST /api/workspaces/{workspaceId}/discovery/local` workflow remains compatible. The new endpoints are the Release 0.2 evidence-only path and do not generate findings, recommendations, proposed changes, or graph mutations.

## Filtering

Evidence accepts `pluginId`, `evidenceType`, `module`, `package`, `filePath`, `classification`, and `minimumConfidence`.

Observations accept `pluginId`, `observationType`/`category`, `module`, `minimumConfidence`, and `supportingEvidenceId`.

Metrics accept `metricName`, `scope`, and `module`. Diagnostics accept `pluginId` and `severity`. Filters are exact except file-path containment and confidence thresholds; no general query language is provided.

## Evidence navigation and provenance

Evidence views expose repository-relative path, module, package, symbol, line, plugin, observed/inferred classification, numeric and banded confidence, source evidence ids, derivation summary, raw attributes, and unresolved/dynamic-value information. Observations and metrics retain supporting evidence ids. The React detail panel follows those ids and presents path/line locations in copyable form.

Confidence bands are display aids:

- high: 90–100%;
- medium: 70–89%;
- low: below 70%.

Confidence describes support for the stated evidence or derivation. It is not an architecture score.

## Plugin executions and diagnostics

Each plugin view contains identity, name, category, dependencies, status, start/completion times, evidence/observation/metric counts, warnings, errors, and partial-success state. Diagnostics expose parse failures, malformed input, traversal truncation, and other plugin messages. Dynamic/unresolved values also remain visible on their evidence item.

Successful, partial, and failed states use distinct UI badges. A partially completed run remains navigable and retains all valid output.

## Persistence and integrity

File persistence uses:

```text
data/workspaces/{workspaceId}/discovery-runs/{runId}/
  run.json
  plugins.json
  evidence.json
  observations.json
  metrics.json
  diagnostics.json
```

An in-memory adapter is used by in-memory configurations and tests. File reads reconstruct the complete details after application restart, and repository methods always require both workspace and run id. Discovery-run JSON files are included in the existing workspace integrity manifest with relative paths and SHA-256 checksums.

## React experience

The workspace-level Discovery Evidence view provides source-path entry, run history, summary counts, status and warning indicators, and Overview, Plugins, Evidence, Observations, Metrics, and Diagnostics tabs. The evidence explorer supports server filters plus local text search and a provenance panel. Observation language stays neutral: deterministic or heuristic, never smell or violation.

The legacy architecture workflow remains separately accessible for compatibility. Its interpretation artifacts are not presented as Release 0.2 evidence explorer output.

## Limitations

- Runs are synchronous; there is no background job scheduler.
- Local repository paths must be accessible to the API process.
- Evidence lists are not paginated in this local Release 0.2 implementation.
- Source contents are not editable or executed from the explorer.
- Absence observations mean “not detected in supplied evidence,” not proof of runtime absence.
- No overall architecture score, Product reasoning, distributed-monolith or ESB classification, bounded-context inference, or recommendation is produced.

## Release 0.3 boundary

The evidence explorer is the trust foundation for later intelligence. Release 0.3 may interpret persisted Release 0.2 evidence into Product-level findings, risks, scores, and recommendations, but users must remain able to inspect the exact confidence, provenance, and source evidence underneath those conclusions.
