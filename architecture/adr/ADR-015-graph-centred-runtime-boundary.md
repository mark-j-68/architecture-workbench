# ADR-015 — Graph-Centred Runtime Boundary

## Status

Accepted

## Context

Architecture Workbench now defines the `ArchitectureKnowledgeGraph` as the platform core. However, early M2 code introduced a sectioned `ArchitectureModel` and a legacy MCP facade around that model. If API, UI, MCP, discovery, healthchecks, or AI reviewer integrations continue to build on the legacy model, the runtime architecture will drift away from the platform constitution.

The platform needs a clear boundary before adding Spring Boot APIs, UI workflows, persistence, provider adapters, discovery connectors, or live MCP transports.

## Decision

Treat `ArchitectureKnowledgeGraph` as the actual runtime boundary of the platform.

All new runtime-facing modules must depend on the graph boundary:

- API endpoints
- UI application workflows
- MCP tools
- discovery connectors
- healthcheck engines
- projection generators
- AI reviewer orchestration
- review-board workflows
- persistence adapters

The legacy `workbench-core` module and its `ArchitectureModel` are retained only for M2 compatibility and migration. They are no longer canonical.

MCP moves to a graph-centred module. MCP tools may read graph context, find elements, list relationships, validate the graph, generate projections, and create governed review proposals through application services. MCP must not expose raw graph patching or uncontrolled mutation tools.

Projection payloads should be typed DTOs rather than raw maps so downstream API/UI/MCP boundaries have stable contracts.

Audit should use a shared abstraction so graph operations, reviewer operations, review-board workflows, and future persistence adapters emit consistent immutable events.

## Consequences

Positive:

- API, UI, MCP, discovery, healthchecks, and AI reviewers build on the correct canonical model.
- The old sectioned model can be migrated deliberately rather than silently remaining the real runtime core.
- Projection contracts become safer for future API and UI work.
- Review results can be connected to graph concepts such as ArchitectureReview, Decision, Risk, and Evidence.
- Audit consistency improves across modules.

Negative:

- Some legacy code remains temporarily duplicated until migration is complete.
- Existing model-oriented examples and schemas need a migration path.
- More modules exist earlier, but their boundaries are clearer.

## Implementation Notes

M3.1 introduces:

- `platform-audit`
- `knowledge-graph-mcp`
- `review-board`
- typed projection DTOs
- legacy markings for `workbench-core`

No live Claude, OpenAI, Codex, or Gemini calls are introduced. No Spring Boot API, persistence adapter, UI feature, live MCP transport, discovery connector, or healthcheck engine is introduced in this decision.
