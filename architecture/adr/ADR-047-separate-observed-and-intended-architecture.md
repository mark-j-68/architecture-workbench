# ADR-047: Separate Observed and Intended Architecture

## Status

Accepted

## Context

Release 0.3 composes deterministic implementation evidence and retains Product findings, recommendations, scorecards, Review Board decisions, and governed graph changes. The Architecture Knowledge Graph is canonical governed architecture state, while Product analyses are read-only projections. Architecture drift requires comparing implementation evidence with architectural intent, but the current model does not name that distinction explicitly.

Treating discovered implementation as canonical intent would let every code change redefine architecture. Treating documentation as intent without governance would make stale or aspirational text authoritative. Storing intended and observed claims in one undifferentiated graph would hide provenance and epistemic state.

## Decision

Represent observed and intended architecture as separate, comparable projections:

- Observed Architecture is a retained, evidence-backed Product projection derived from discovery, composition, dependencies, and deterministic analysis. It never mutates the canonical graph.
- Intended Architecture is a Product-scoped projection of accepted Architecture Knowledge Graph elements and relationships, accepted architecture constraints, and explicitly governed ADR-linked intent.
- Inferred candidates and unaccepted proposals belong to neither projection as accepted facts.

Use immutable `ArchitectureSnapshot` manifests to freeze comparable observed and intended projections. Freeze the Product-relevant intended subgraph as a normalized content-addressed artifact so historical comparisons do not follow the mutable current graph. Retain source evidence by reference.

Use `ArchitectureDelta` to compare observed snapshots over time, intended state over time, or intended with observed. A difference becomes drift only when explicit applicable intent, sufficient observability, and a deterministic contradiction rule exist. Missing evidence remains unknown.

The Architecture Knowledge Graph remains canonical for governed intent. Discovery and analysis remain authoritative for what was observed. Neither silently overwrites the other.

## Impact on graph semantics

Graph elements and relationships require stable Product scope, directly or through governed containment, to participate in intended Product projections. Accepted semantic graph relationships use a controlled taxonomy and endpoint validation. Provenance metadata distinguishes accepted intent sources and links decisions/ADRs, but confidence is not applied to accepted intent.

Observed assertions may reference matching graph IDs for comparison. A match is traceability, not graph membership or acceptance.

## Relationship to governance

Recommendations, Review Board approval, and candidate approval do not mutate intent. Any change to intended elements, relationships, or constraints must become a concrete `ProposedArchitectureChange` and be explicitly accepted through the existing graph mutation boundary. Observed implementation cannot automatically redefine intended architecture.

If an architect decides that observed divergence is the desired future state, the decision produces a proposal to revise intent. Earlier snapshots and drift findings remain historical records.

## Alternatives considered

### Store observed implementation directly in the canonical graph

Rejected because discovery would mutate governed intent, evidence refreshes could silently rewrite architecture, and inferred semantics would appear accepted.

### Use one graph with only an `observed/intended` attribute

Rejected as the primary model because mixed queries and lifecycle operations could conflate epistemic states, and observed refresh would still share a mutation boundary with intended state. Normalized assertion projections may share shapes, but their stores and authorities remain distinct.

### Treat ADR and documentation text as authoritative intent

Rejected because documents may be stale, ambiguous, aspirational, or discovered from an untrusted source. ADRs contribute intent only through explicit governed links.

### Derive historical architecture by replaying audit events

Rejected because Release 0.4 does not adopt event sourcing. Immutable snapshots provide temporal comparison without making events the state store.

## Consequences

- Drift has a precise comparison boundary and cannot be inferred from change alone.
- Users can inspect observed evidence and governed intent side by side.
- Product graph scoping and constraint governance must become explicit.
- Snapshot persistence stores normalized, content-addressed projection artifacts in addition to manifests.
- Some comparisons return unknown when observability or identity is insufficient.
- The canonical graph remains smaller and governed rather than becoming a mirror of all discovered implementation facts.
