# ADR-016 — Workspace and Repository Boundary Before Persistence

## Status

Accepted

## Context

Architecture Workbench now treats `ArchitectureKnowledgeGraph` as the canonical runtime boundary. Before adding REST APIs, UI workflows, database persistence, file persistence, or discovery ingestion, the platform needs a clean workspace and repository boundary.

A workspace represents one architecture initiative, product, system, or imported codebase. Every graph must belong to a workspace so graph operations can be scoped, audited, exported, imported, and eventually persisted consistently.

Choosing a database too early would risk coupling the domain model to the wrong storage shape. The graph model, workspace lifecycle, snapshot contracts, audit semantics, and import/export boundary should stabilize first.

## Decision

Introduce a `workspace-service` module with:

- `WorkspaceId`
- `Workspace`
- `WorkspaceMetadata`
- `WorkspaceRepository`
- `InMemoryWorkspaceRepository`
- `ArchitectureGraphRepository`
- `InMemoryArchitectureGraphRepository`
- graph snapshot import/export contracts
- application services for workspace and graph lifecycle operations

The module supports:

- create workspace
- rename workspace
- list workspaces
- get workspace graph
- save workspace graph
- import initial graph snapshot
- export graph snapshot

Workspace and graph operations emit audit events through the shared `platform-audit` abstraction.

No database, file-system, or framework persistence is introduced in this milestone.

## Consequences

Positive:

- API and UI work can depend on stable workspace and graph repository contracts.
- Each graph has a workspace owner before persistence is introduced.
- Import/export snapshot contracts are explicit.
- Audit events exist before repository adapters become durable.
- Persistence choices remain open.

Negative:

- In-memory repositories are not durable.
- Snapshot import/export contracts may need versioning before production use.
- Repository implementations will need replacement adapters later.

## Deferred Decisions

The following are deliberately deferred:

- JPA
- Spring Data
- Neo4j
- PostgreSQL
- files-on-disk persistence
- object storage
- event-store persistence
- graph database schema design

These should be decided only after the graph and workspace contracts have been exercised by API, MCP, healthcheck, discovery, and review-board workflows.
