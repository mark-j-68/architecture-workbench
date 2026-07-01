# ADR-021 - Typed Architecture Events Before Event Sourcing

## Status

Accepted

## Context

The Architecture Kernel Design Pack defines architecture events as part of the
kernel contract. Before this decision, implemented workflows emitted free-form
audit action strings such as `WORKSPACE_CREATED`,
`ARCHITECTURE_ELEMENT_CREATED`, `DISCOVERY_RUN_STARTED`, and
`ARCHITECTURE_REVIEW_RUN_COMPLETED`.

Those strings were useful for early audit coverage, but they were not a stable
kernel contract. They did not consistently expose source, workspace scope,
causation id, correlation id, audit relevance, mutation target, actor, payload,
or protected payload reference.

## Decision

Introduce typed architecture events before introducing event sourcing.

The shared `platform-audit` boundary now defines:

- `ArchitectureEvent`
- `ArchitectureEventEnvelope`
- `ArchitectureEventType`
- `ArchitectureEventSource`
- `CausationId`
- `CorrelationId`
- `AuditRelevance`
- `MutationTarget`
- `Actor`
- `ActorType`

Implemented workflows emit typed architecture events for:

- `WorkspaceCreated`
- `GraphImported`
- `ElementAdded`
- `RelationshipAdded`
- `ProjectionGenerated`
- `DiscoveryStarted`
- `DiscoveryCompleted`
- `ReviewRequested`
- `ReviewCompleted`

Typed architecture events are mapped into the existing append-only,
hash-chained audit sink. The audit log remains the retention mechanism for now.

## Consequences

Typed events are now the kernel contract for implemented workflows. Providers,
APIs, UI surfaces, MCP tools, discovery workflows, generation workflows, and
review workflows must depend on typed architecture events rather than free-form
audit strings when they interact with kernel-governed behavior.

The existing audit sink remains append-only and hash-chained. This preserves the
regulatory audit direction without forcing storage or replay decisions.

Event sourcing is deliberately deferred. The platform does not yet require
replay, snapshotting, upcasting, durable event stores, ordering guarantees, or
idempotent command handling.

Persistence is deliberately deferred. No database, file event store, JPA,
Spring Data, Neo4j, or PostgreSQL dependency is introduced by this decision.

Provider, API, and UI work must treat typed architecture events as the semantic
contract, but must not introduce live providers, REST APIs, or UI runtime
behavior as part of this milestone.

## Boundaries

This decision does not implement events for future workflows such as provider
invocation, MCP tool invocation, hypotheses, recommendations, decisions,
experiments, outcomes, learnings, or patterns. Those event names may exist in
the taxonomy, but they must not be emitted until their workflows are governed by
validated application services.

This decision does not depend on `workbench-core`.
