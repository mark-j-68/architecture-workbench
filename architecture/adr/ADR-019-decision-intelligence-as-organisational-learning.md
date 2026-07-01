# ADR-019 - Decision Intelligence as Organisational Learning

## Status

Accepted

## Context

The Architecture Intelligence Model establishes the evidence, observation, finding,
recommendation, and decision concepts needed for review and governance. That is
not enough for an AI-native architecture platform: the platform also needs to
learn from what happened after a decision was made.

Architecture recommendations should become explicit hypotheses that can be
validated, tested through experiments, compared against actual outcomes, and
distilled into reusable organisational knowledge.

## Decision

Introduce a `decision-intelligence` module that extends Architecture
Intelligence with immutable decision-learning concepts:

- `Hypothesis`
- `Experiment`
- `Outcome`
- `Learning`
- `Pattern`

The module provides services to:

- create and validate hypotheses
- record experiments
- compare expected and actual outcomes
- derive learnings from experiment outcomes
- derive reusable architectural patterns from learnings

Traceability is mandatory across the learning chain:

`Evidence -> Observation -> Finding -> Hypothesis -> Recommendation -> Decision -> Experiment -> Outcome -> Learning`

The initial implementation remains provider-neutral and in-memory only. It does
not introduce live AI providers, persistence, REST APIs, or UI concerns.

## Consequences

Architecture Workbench can now represent architectural learning as a first-class
domain capability rather than as free-text commentary attached to ADRs.

This creates a foundation for future healthchecks, review boards, governance
analytics, and AI-assisted recommendations to reuse prior outcomes and patterns.

The confidence model remains deliberately simple for now. Future milestones can
refine confidence scoring using richer evidence weighting, runtime metrics,
reviewer disagreement, and longitudinal outcome data.

## Boundaries

The `decision-intelligence` module depends on `architecture-intelligence`.
It does not depend on API, UI, MCP transport, persistence, or provider-specific
AI integrations.
