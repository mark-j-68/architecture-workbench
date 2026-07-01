# ADR-005 — Introduce Deterministic Validation Engine

## Status

Accepted

## Context

AI-generated architecture artefacts can be useful but may be inconsistent or incomplete. Architecture Workbench should not rely only on AI review.

The platform needs deterministic, repeatable checks that can run after every model change.

## Decision

Architecture Workbench shall include a validation engine with rule-based checks over the ArchitectureModel.

Initial rule categories:

- DDD completeness
- command/event consistency
- aggregate ownership
- service boundary checks
- naming conventions
- context map consistency
- read model population
- messaging completeness
- security readiness

## Consequences

Positive:

- Higher trust in generated artefacts
- Repeatable quality checks
- Better AI review context
- Foundation for architecture fitness functions

Negative:

- Requires ongoing rule catalogue maintenance
- Some architectural judgement cannot be fully deterministic
