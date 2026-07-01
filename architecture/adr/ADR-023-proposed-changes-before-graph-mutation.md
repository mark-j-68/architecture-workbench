# ADR-023 - Proposed Changes Before Graph Mutation

## Status

Accepted

## Context

Discovery, Review Board workflows, MCP tools, providers, and future AI agents can
produce useful architecture intelligence. That intelligence must not directly
mutate the canonical Architecture Knowledge Graph.

The platform requires a governed boundary between evidence-backed intelligence
and graph mutation. Without that boundary, discovery interpretations, reviewer
recommendations, or agent output could silently become canonical architecture
state without explicit acceptance.

## Decision

All intelligence outputs that imply graph changes must become proposed changes
first.

Proposed changes are immutable domain records that carry:

- workspace id
- correlation id
- proposed mutation type
- proposed mutation payload
- proposed change status
- traceability to AIM recommendation
- traceability to AIM findings
- traceability to evidence

Only accepted proposed changes may mutate the Architecture Knowledge Graph.
Acceptance must route through validated graph services such as
`ArchitectureElementService` and `RelationshipService`.

Rejected and deferred proposed changes must not mutate the graph.

## Consequences

Discovery can propose graph element additions from evidence-backed findings, but
does not apply them directly.

Review Board consensus and recommendation candidates can propose graph
relationships, but do not apply them directly.

Accepted proposals emit the existing typed graph mutation events where
available, such as `ElementAdded` and `RelationshipAdded`.

This keeps the graph canonical while preserving traceability from evidence to
observation, finding, recommendation, proposed change, and accepted graph
mutation.

## Boundaries

This decision does not introduce AI providers, persistence, REST APIs, UI,
event sourcing, or broad proposed-change event sourcing.

Future MCP tools and provider integrations must submit proposed changes rather
than applying graph mutations directly.
