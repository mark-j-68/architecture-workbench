# ADR-011 — Architecture Knowledge Graph as Platform Core

## Status

Accepted

## Context

Architecture Workbench began with a canonical `ArchitectureModel` and deterministic projections. That was sufficient for M2, but the product direction now requires a richer foundation:

- Design Mode for target architecture creation.
- Discovery Mode for existing-system analysis.
- DDD validation, healthchecks, C4, BPMN, DMN, ADRs, OpenAPI, Event Storming, and React Flow projections.
- AI Architecture Review Board records with decisions, risks, evidence, disagreements, and audit traceability.
- MCP-based agent collaboration without direct agent mutation of the model.

A sectioned document model is too limiting for this because architecture knowledge is inherently relational. Decisions trace to evidence. Risks are mitigated by decisions. Commands are handled by aggregates. Components realize capabilities. Reviews cite findings and evidence. Projections need to traverse those relationships without creating disconnected artefact stores.

## Decision

Architecture Workbench will use a governed `ArchitectureKnowledgeGraph` as the platform core.

The graph contains typed architecture elements:

- DomainEvent
- Command
- Aggregate
- BoundedContext
- Capability
- Policy
- Decision
- Risk
- System
- Container
- Component
- ADR
- ArchitectureReview
- Evidence

The graph also contains typed relationships such as contains, handled_by, emits, governed_by, supports, realized_by, depends_on, mitigates, documented_by, reviewed_by, evidenced_by, and traces_to.

Event Storming, React Flow, C4, BPMN, DMN, ADRs, OpenAPI, AI reviews, and healthchecks are projections from the graph. They are not independent sources of truth.

All mutations must be routed through validated application services. Every accepted mutation must emit an immutable audit event.

## Consequences

Positive:

- Architecture knowledge becomes navigable and traceable.
- Design, discovery, validation, generation, and review share one domain model.
- AI prompts can be grounded in explicit graph context and cited evidence.
- Healthchecks can produce risks and decisions instead of isolated reports.
- Projection drift is reduced because artefacts derive from the same graph.

Negative:

- The model is more sophisticated than a sectioned YAML document.
- Persistence must handle graph elements, relationships, evidence, and audit events.
- Existing M2 model classes need a migration path rather than abrupt replacement.

## Implementation Boundary

M3 establishes the `architecture-knowledge-graph` module and its application service boundaries. It does not implement live agent calls, provider integrations, or autonomous graph mutation.
