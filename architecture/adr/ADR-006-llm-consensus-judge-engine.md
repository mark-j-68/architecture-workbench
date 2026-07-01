# ADR-006 — Use AI Architecture Review Board for AI-Assisted Decisions

## Status
Accepted, refined by ADR-013

## Context
Architecture Workbench will use AI to extract domain models, review architecture, generate ADR drafts, propose remediations, and prepare instructions for code-building agents. In a regulated environment, a single-model judgement is too opaque and too vulnerable to model-specific bias, hallucination, or prompt interpretation errors.

M3 reframes these decisions as Review Board records over the architecture knowledge graph. The board records assessments, disagreements, consensus recommendations, linked risks, linked evidence, generated ADR drafts, and decision outcomes.

## Decision
Introduce an AI Architecture Review Board as a first-class governance subsystem. Material AI-assisted decisions will be represented as graph-backed review records. Reviewers may include Claude and OpenAI/Codex in later milestones, but M3 defines only the model, evidence links, traceability, and governance boundary.

Each reviewer assessment records a verdict, confidence, rationale reference, risks, evidence references, and proposed corrections. A consensus coordinator compares verdicts and either accepts the proposal, rejects it, requests revision, or escalates to human review.

Consensus must be bounded by a maximum number of rounds. The default is three rounds. The system must never enter an unbounded self-correction loop.

Agents and reviewers must not directly mutate the architecture knowledge graph. Accepted changes are applied only through validated application services.

## Consequences
- AI outputs become reviewable decisions, not invisible magic.
- Disagreement between models becomes a signal requiring revision or human review.
- All judge prompts, responses, tool usage, model identifiers, evidence links, and outcomes must be recorded in the immutable activity log.
- Cost and latency increase, so consensus should apply to material decisions rather than every trivial transformation.

## Initial Decision Types Requiring Consensus
- Domain extraction from Event Storming images.
- Architecture review findings above low severity.
- ADR generation and ADR amendment.
- Existing-system healthcheck findings above low severity.
- Regulatory interpretation or compliance-related recommendations.
- Claude Code / agent instruction generation that could materially affect implementation.
