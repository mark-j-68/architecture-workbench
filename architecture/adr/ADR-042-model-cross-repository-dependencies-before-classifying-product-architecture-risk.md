# ADR-042: Model Cross-Repository Dependencies Before Classifying Product Architecture Risk

## Status

Accepted

## Context

Product composition exposes repository evidence and exact shared identities. Later analysis needs contract, artifact, release, deployment, and ownership relationships, but presenting incomplete correlations as architecture risk would weaken traceability and trust.

## Decision

Represent Product dependencies neutrally and retain their source evidence, discovery run, repository, confidence, classification, and derivation. Show deterministic literal-version contradictions explicitly. Treat missing or dynamic compatibility as unknown, never incompatible. Record release coordination evidence before any lockstep classification. Retain every dependency-composition version without adopting event sourcing.

Distributed-monolith classification, ESB/router smells, architecture scoring, bounded-context inference, and recommendations remain deferred to later Release 0.3 milestones.

## Consequences

Users can inspect and filter an evidence-backed Product dependency graph, compatibility matrix, releases, deployments, and ownership diagnostics. Later intelligence can consume stable versioned inputs while remaining responsible for architectural judgement.
