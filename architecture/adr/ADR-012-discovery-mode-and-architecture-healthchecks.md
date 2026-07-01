# ADR-012 — Discovery Mode and Architecture Healthchecks

## Status

Accepted

## Context

Architecture Workbench must support more than greenfield design. Teams also need to inspect existing systems, understand their real architecture, detect architectural risks, and plan remediation. Existing-system analysis should not produce a disconnected report that immediately goes stale.

Discovery output needs to become part of the same governed architecture knowledge graph used for Design Mode.

## Decision

Introduce Discovery Mode as a first-class product mode.

Discovery Mode ingests existing-system evidence from sources such as:

- repository structure
- code ownership and dependency data
- OpenAPI, AsyncAPI, BPMN, and DMN artefacts
- C4 or architecture documentation
- infrastructure definitions
- runtime configuration
- operational signals
- prior ADRs and review notes

The ingested data is represented as graph elements and relationships. Evidence is stored as first-class `Evidence` nodes. Healthcheck findings are represented as `Risk`, `ArchitectureReview`, `Decision`, and evidence-linked relationships rather than standalone reports.

Healthchecks run deterministic rules over the graph before any AI review. Example healthchecks include:

- missing or stale ADR evidence
- unowned systems or containers
- commands without aggregate handlers
- domain events without emitters
- undocumented external dependencies
- API contracts without owning capabilities
- deployment resources without owning systems
- critical risks without mitigation decisions

## Consequences

Positive:

- Existing-system understanding becomes durable architecture knowledge.
- Healthcheck findings can be traced to evidence and remediation decisions.
- Discovery Mode and Design Mode share the same governance model.
- AI review can cite concrete evidence instead of relying on unstructured summaries.

Negative:

- Discovery connectors must classify evidence carefully.
- Healthcheck rules need confidence and provenance metadata.
- Some existing systems will contain contradictory evidence that requires human review.

## Scope

M3 defines Discovery Mode concepts, graph representation, healthcheck categories, and traceability requirements. It does not implement repository scanners, runtime collectors, or AI provider calls.
