# ADR-034 - Pluggable Evidence-First Discovery Engine

## Status

Accepted

## Context

Release 0.2 is responsible for deterministic architecture discovery and
evidence collection. Release 0.3 has been planned as Multi-Repo Product
Architecture Intelligence.

The platform needs a clean boundary between observing systems and interpreting
architecture. Without that boundary, the Discovery Engine risks becoming a
monolithic scanner that mixes parsing, heuristics, architectural judgement,
recommendations, and graph mutation.

## Decision

Discovery is plugin-based and evidence-first.

Evidence is the primary output of discovery. Deterministic plugins may also
produce narrowly scoped structural observations, but broad architectural
findings and recommendations belong to the Architecture Intelligence Model.

Discovery plugins must be independently identifiable, versioned, auditable, and
traceable. Plugin outputs must include provenance and confidence.

AI-assisted interpretation is optional and separate from deterministic
discovery. AI-assisted outputs must be labelled as such and must not masquerade
as observed facts.

Discovery does not directly mutate the canonical graph. Inferred graph changes
must use the existing Proposed Change workflow and require explicit acceptance
before graph mutation.

Release 0.2 provides evidence consumed by Release 0.3. Release 0.3 composes
single-repository discovery outputs into product-level architecture
intelligence, including modularity, release independence, contract maturity, and
distributed monolith risk.

## Consequences

The Discovery Engine remains reusable across:

- single repository discovery
- future multi-repository discovery
- multiple technology stacks
- deterministic plugins
- future runtime and cloud discovery
- future AI-assisted discovery plugins

The Architecture Intelligence Model remains the place for findings,
recommendations, product architecture judgement, and proposed changes.

The Review Board remains the governance point for recommendations and proposed
changes.

The Knowledge Graph records accepted architectural state only.

## Boundaries

This ADR does not implement runtime code.

It does not add:

- Java implementation
- Maven changes
- API changes
- UI changes
- live AI providers
- database persistence
- event sourcing

Future implementation must preserve the principle:

```text
Discovery observes.
Architecture Intelligence interprets.
Review Board governs.
Knowledge Graph records accepted state.
```
