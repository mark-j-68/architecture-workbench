# ADR-017 — Discovery Mode as Architecture Reverse Engineering

## Status

Accepted

## Context

Architecture Workbench must support existing systems as well as green-field design. Existing systems often have partial documentation, stale diagrams, implicit dependencies, missing ADRs, and runtime behaviour that differs from stated architecture.

The platform constitution defines Discovery Mode as the reverse-engineering and healthchecking side of the platform. Discovery must produce governed architecture knowledge, not disconnected reports.

## Decision

Introduce a `discovery-engine` module as the foundation for Discovery Mode.

The first implementation supports local repository discovery only. It scans a project directory for shallow architecture signals:

- Maven modules
- `pom.xml` files
- Java packages
- Spring controllers
- Spring services
- repository classes
- configuration files
- Docker files
- README and documentation files
- ADR directories
- test directories

Discovered artifacts are mapped into `ArchitectureKnowledgeGraph` nodes where reasonable. This is intentionally shallow: the connector identifies architecture-relevant evidence without attempting deep static analysis.

Discovery healthchecks initially identify:

- missing README
- missing tests
- missing ADR directory
- multiple modules detected
- Spring controllers detected
- Dockerfile detected
- no obvious API layer
- no obvious domain layer

Discovery runs and findings emit audit events through the shared platform audit abstraction.

## Consequences

Positive:

- Discovery Mode now has a real module boundary.
- Existing-system evidence can enter the architecture knowledge graph.
- Healthcheck findings are available before any AI review.
- The implementation remains local and deterministic.
- Future repository, API, infrastructure, and runtime scanners can implement the same connector interface.

Negative:

- Shallow scanning can miss architectural intent.
- Java/Spring detection is heuristic and not a compiler-backed static analysis.
- The first healthchecks are intentionally coarse.

## Deferred Decisions

The following remain out of scope:

- external scanners
- live AI interpretation
- REST APIs
- UI workflows
- durable persistence
- deep static analysis
- runtime system inspection
- source control provider integration

These should be added only after the discovery domain contracts, audit behaviour, graph mapping, and workspace boundary remain stable under real usage.
