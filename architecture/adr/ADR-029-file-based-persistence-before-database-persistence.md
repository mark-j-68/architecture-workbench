# ADR-029 - File-Based Persistence Before Database Persistence

## Status

Accepted

## Context

The M5 product slice exposes a demoable Architecture OS workflow through a
Spring Boot API and React UI, but in-memory state is lost when the backend
restarts. That makes local demos and iterative development unnecessarily
fragile.

The kernel contracts are still evolving. Choosing PostgreSQL, Neo4j, JPA,
Spring Data, or event sourcing now would harden infrastructure decisions before
the workspace, graph, proposed-change, review, and audit boundaries are stable.

## Decision

Add file-backed persistence adapters for local runs before introducing database
persistence.

Persist local JSON under:

```text
./data/workspaces
```

with a configurable storage root via:

- `architecture.workbench.storage.dir`
- `ARCHITECTURE_WORKBENCH_STORAGE_DIR`

The API uses file-backed repositories by default and keeps the in-memory option
available through:

```text
architecture.workbench.persistence=in-memory
```

The persisted adapter state includes workspace metadata, graph snapshots,
proposed changes, review-board session snapshots, and audit/event records where
the current service boundaries expose them.

## Consequences

The local demo workflow can survive backend restart for the durable parts of
the M5 flow:

- workspaces
- graph state
- accepted proposed-change results
- proposed-change decisions
- review-board session snapshots
- audit/event records

JSON files are an adapter format only. They are not the canonical architecture
domain model, and they must not introduce business rules.

Database persistence remains deferred until kernel contracts and workspace
ownership semantics are stable.

Event sourcing remains deferred. The existing immutable audit log remains
append-only and hash-chained, but it is not used to rebuild runtime state.

## Boundaries

This decision does not add:

- PostgreSQL, Neo4j, or any other database
- JPA or Spring Data
- event sourcing
- multi-process file locking
- schema migration tooling
- live AI providers
- authentication or authorization

Future database adapters must preserve the current repository boundaries and
must not bypass validated application services, proposed-change governance, or
typed architecture event emission.
