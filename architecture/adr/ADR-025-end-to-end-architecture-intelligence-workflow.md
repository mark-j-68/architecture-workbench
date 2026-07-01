# ADR-025 - End-to-End Architecture Intelligence Workflow

## Status

Accepted

## Context

Architecture Workbench has introduced the Architecture Knowledge Graph,
Architecture Intelligence Model, Discovery Mode, proposed changes, typed kernel
events, and a governed Review Board workflow.

Those capabilities must operate as one coherent Architecture OS kernel flow
before APIs, UI panels, live AI providers, persistence, or MCP mutation tools
are added. The critical risk is allowing discovery outputs, reviewer outputs,
or future provider outputs to bypass evidence-backed intelligence and mutate the
canonical graph directly.

## Decision

Adopt the following canonical workflow:

Discovery / Review
-> Evidence
-> Observation
-> Finding
-> Recommendation
-> Proposed Change
-> Review Board
-> Explicit Acceptance
-> Graph Mutation

Discovery and review outputs become AIM objects before they can influence graph
state. Recommendation candidates produce proposed architecture changes. Proposed
changes are reviewed by the Review Board, which recommends acceptance,
rejection, deferral, further discovery, or further review.

The Review Board does not mutate the graph. Graph mutation only occurs when a
caller explicitly accepts a proposed change through `ProposedChangeService`,
which routes the mutation through validated graph application services.

Implemented workflow events continue to use typed architecture event envelopes
and the append-only hash-chained audit sink. Event sourcing remains deferred.

## Consequences

The platform now has an executable kernel workflow that proves discovery,
intelligence, governance, explicit acceptance, graph mutation, and audit can
work together without live AI providers or persistence.

The workflow preserves these invariants:

- evidence precedes observations, findings, recommendations, and proposed
  changes
- proposed changes must trace to recommendation, finding, evidence, workspace,
  and correlation id
- review board decisions must trace to session inputs and participant votes
- rejected, deferred, and more-evidence outcomes do not mutate the graph
- accepted proposed changes mutate the graph only through validated services
- typed architecture events retain workspace, causation, correlation, audit
  relevance, mutation target, and hash-chain continuity

Future REST APIs, UI panels, MCP tools, discovery connectors, provider adapters,
and persistence adapters must implement this workflow rather than creating
parallel mutation paths.

## Boundaries

This decision does not introduce live AI providers, persistence, REST APIs, UI,
event sourcing, or automatic graph mutation from Review Board decisions.

The current implementation is proved through end-to-end tests using in-memory
repositories, in-memory audit, local repository discovery, reviewer workflow
stubs, proposed changes, and validated graph services.
