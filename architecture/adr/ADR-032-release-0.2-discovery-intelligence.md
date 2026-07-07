# ADR-032 - Release 0.2 Discovery Foundations

## Status

Accepted

## Context

Architecture Workbench has established the Architecture OS kernel foundation:

- canonical Architecture Knowledge Graph
- Architecture Intelligence Model
- Discovery Mode foundation
- proposed changes before graph mutation
- governed Review Board workflow
- thin API and UI shells
- local file persistence with integrity and recovery

The next release should shift from foundation-building to a coherent
capability-focused product experience.

## Decision

Define Release 0.2 as Discovery Foundations and Evidence Collection.

Release 0.2 focuses on existing Java/Spring Boot systems. The user should be
able to create a workspace, point the platform at a local Spring Boot project,
run discovery, inspect evidence-backed structural observations, and pass those
observations into the Architecture Intelligence Model for findings,
recommendations, proposed changes, governance, and graph updates.

The release scope includes:

- Java/Spring Boot project analysis
- Maven module discovery
- package dependency detection
- Spring controller/service/repository detection
- inferred layers
- inferred architecture style
- basic bounded context candidates
- DDD smells
- coupling and cycle detection
- architecture score inputs for the intelligence layer
- evidence-backed recommendations

The release explicitly excludes:

- live AI providers
- database persistence
- cloud runtime scanning
- GitHub PR integration
- meeting capture
- event sourcing

## Consequences

Release 0.2 becomes the first capability-focused release after the kernel
foundation, but its capability is discovery and evidence collection rather than
high-level architectural judgement.

The North Star demo becomes the product alignment tool for near-term
implementation. Work that does not support the Discovery Foundations demo is
deferred unless it protects kernel correctness, local developer experience, or
traceability.

Discovery output must continue to flow through:

Evidence -> Observation -> Finding -> Recommendation -> Proposed Change ->
Review Board -> Explicit Acceptance -> Graph Mutation.

Discovery must not directly mutate the graph, and Review Board decisions must
not automatically apply proposed changes.

## Boundaries

Release 0.2 remains local and provider-neutral. It does not require live AI
providers, production persistence, event sourcing, cloud scanning, or external
collaboration integrations.

Release 0.3 consumes Release 0.2 evidence and observations for Multi-Repo
Product Architecture Intelligence.

Future releases build on this:

- v0.3 AI Review Board
- v0.4 Continuous Architecture
- v0.5 Provider Ecosystem
