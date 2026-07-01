# ADR-018 — The Architecture Intelligence Model

## Status

Accepted

## Context

Architecture Workbench now has a governed architecture knowledge graph, discovery foundations, provider-neutral reviewer contracts, review-board workflows, and audit boundaries. These modules identify and connect architectural facts, but the platform also needs a shared intelligence model for reasoning over evidence, observations, findings, concerns, recommendations, decisions, metrics, trends, and reviewers.

Without a common intelligence model, discovery, healthchecks, review plugins, governance workflows, and future AI providers would each create their own incompatible concepts.

## Decision

Introduce an `architecture-intelligence` module containing the Architecture Intelligence Model (AIM).

AIM defines immutable domain concepts:

- Evidence
- Observation
- Finding
- Concern
- Recommendation
- DecisionOutcome
- Metric
- Trend
- Reviewer

AIM enforces traceability:

- every observation traces to evidence;
- every finding traces to observations;
- every recommendation traces to findings;
- every decision traces to recommendations and evidence;
- metrics trace to evidence.

The module also provides services for:

- promoting observations into findings;
- promoting findings into recommendations;
- recording decisions;
- attaching evidence;
- calculating confidence;
- associating recommendations with concerns.

## Consequences

Positive:

- Discovery, review, recommendation, governance, and future AI capabilities share one intelligence vocabulary.
- Traceability becomes a domain invariant rather than a UI/reporting afterthought.
- AI providers can later plug into the same model without owning core concepts.
- Confidence and decision lineage become explicit.

Negative:

- The platform now has a separate intelligence model alongside the graph model.
- Integration work is needed to map discovery findings and review-board records into AIM.
- Confidence calculations are intentionally simple initially and will need refinement.

## Scope

This decision introduces the AIM domain module only.

It does not implement AI providers, persistence, REST APIs, UI workflows, or live reviewer calls.
