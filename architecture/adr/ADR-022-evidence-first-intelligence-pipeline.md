# ADR-022 - Evidence-First Intelligence Pipeline

## Status

Accepted

## Context

Discovery Mode and Review Board workflows produce useful architecture signals.
If those signals bypass the Architecture Intelligence Model, the platform loses
the evidence-backed traceability required by the Architecture Kernel.

Discovery artifacts are observations about an existing system. Review findings
are reviewer interpretations of supplied architecture context. Both must become
evidence-backed intelligence objects before they influence findings,
recommendations, decisions, or generated artefacts.

## Decision

Discovery and Review Board outputs must flow through the Architecture
Intelligence Model.

Discovery follows this path:

`DiscoveredArtifact -> Evidence -> Observation -> Finding`

Healthcheck DTOs remain boundary objects, but governed healthcheck findings are
mapped into AIM `Finding` objects backed by observations and evidence.

Review Board findings are normalized into AIM `Finding` objects. Review
consensus can produce an AIM `Recommendation` candidate, but it does not create
decisions automatically.

Typed architecture events remain the workflow-level contract. Discovery runs
continue to emit `DiscoveryStarted` and `DiscoveryCompleted`; review workflows
continue to emit `ReviewRequested` and `ReviewCompleted`.

## Consequences

Discovery no longer mutates the architecture graph directly as an
interpretation step. Discovery records evidence and intelligence first. Later
governed workflows may accept proposed graph changes through validated
application services.

Review Board outputs become traceable AIM records rather than isolated review
DTOs or direct graph decisions.

Recommendation candidates are explicitly not decisions. Human or governed
decision workflows must accept, reject, or defer recommendations later.

This decision does not introduce AI providers, persistence, REST APIs, UI,
event sourcing, or automatic graph mutation from discovery interpretation.

## Boundaries

Discovery-specific DTOs and review-specific DTOs remain useful at module
boundaries, but they must be normalized into AIM objects before they are used as
governed architecture intelligence.

No `RecommendationProposed`, `DecisionRecorded`, provider, or MCP tool events
are emitted by this milestone.
