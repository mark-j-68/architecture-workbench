# ADR-037 - Spring Discovery Produces Framework Evidence, Not Architectural Judgement

## Status

Accepted

## Context

ADR-034 establishes an evidence-first Discovery Engine. ADR-035 adds repository
and Maven facts, and ADR-036 adds deterministic Java structure and package
relationships. Spring applications expose substantial framework structure in
annotations, method signatures, configuration files, generic repository types,
and direct client calls.

Those markers are useful architectural inputs, but interpreting a controller,
transaction boundary, repository, or message listener as good or bad design
would mix discovery with architectural judgement. Running a Spring context or
requiring compilable source would also make repository discovery unsafe and
non-repeatable.

## Decision

Implement deterministic v0.2.3 Spring Application, Web, Component, Data,
Transaction, and Messaging discovery plugins.

The plugins use lightweight source and configuration scanning. They record:

- Spring Boot runtime entry points, configuration, component scans, and profiles
- controllers, advice, endpoint mappings, and direct DTO references
- component stereotypes, bean methods, injection points, and interfaces
- Spring Data repository generic types, entities, identifiers, tables, and queries
- explicit transaction boundaries and declared settings
- messaging listeners, publishing abstractions, EventBridge usage, and static destinations

Every item retains repository-relative provenance, module, package, symbol,
line, marker, plugin id, confidence, and observed/inferred classification.
Explicit framework declarations are observed at `1.0`. Static composition and
type-based relationships are narrow deterministic inferences at high
confidence. Dynamic expressions are retained with reduced confidence and
explicit uncertainty.

The plugins emit AIM Evidence and factual Observations only. They do not emit
findings, risks, recommendations, proposed graph changes, or architecture
judgements.

`LocalRepositoryDiscoveryConnector` delegates Spring-specific scanning to the
new plugins and adapts only evidence with an existing legacy artifact type.

## Consequences

Spring application facts become repeatable, traceable inputs to later
intelligence without executing untrusted application code.

Non-Spring, dependency-incomplete, and syntactically imperfect repositories can
still complete discovery with empty or partial Spring evidence.

Lightweight parsing cannot resolve meta-annotations, runtime property values,
custom composed mappings, generated code, or full symbol ownership. These
limitations are explicit rather than filled by guesses.

Future Architecture Intelligence stages may correlate the evidence to assess
modularity, ownership, coupling, or architecture risk, but must keep those
conclusions separate and cite the deterministic evidence.

## Boundaries

This decision does not add:

- bounded-context inference
- architecture-style classification
- distributed-monolith analysis
- ESB or router smell detection
- OpenAPI generation
- database schema analysis
- event ownership or orchestration inference
- Spring context loading or application execution
- AI providers
- REST API, UI, or persistence changes
- event sourcing

Discovery observes framework facts. Architecture Intelligence interprets them.
