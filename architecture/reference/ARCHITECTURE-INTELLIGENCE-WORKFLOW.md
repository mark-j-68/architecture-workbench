# Architecture Intelligence Workflow

This document defines the canonical end-to-end workflow for Architecture
Workbench intelligence. The workflow connects Discovery Mode, Architecture
Intelligence Model objects, proposed graph changes, the Review Board, explicit
acceptance, graph mutation, and immutable audit.

The workflow is intentionally evidence-first and proposal-based. Discovery,
reviewers, MCP tools, and future AI providers may interpret architecture state,
but they do not directly mutate the Architecture Knowledge Graph.

## Canonical Flow

Discovery / Review
-> Evidence
-> Observation
-> Finding
-> Recommendation
-> Proposed Change
-> Review Board
-> Explicit Acceptance
-> Graph Mutation

## Workflow Stages

### 1. Discovery Or Review

Discovery Mode inspects an existing system, repository, document set, API,
diagram, infrastructure source, or runtime source. Review workflows inspect the
current graph, projections, evidence, recommendations, and proposed changes.

The input to the intelligence workflow is always treated as source material,
not as canonical graph truth.

### 2. Evidence

Discovered artifacts and reviewer outputs become AIM Evidence first.

Evidence records source, provenance, confidence, timestamp, references, and
supporting artifacts. Evidence is factual source material. Interpretation is
not stored directly as evidence.

### 3. Observation

Observations are derived from evidence.

Every observation must reference evidence. An observation may describe that a
repository contains Spring controllers, Maven modules, configuration files, test
directories, ADR directories, or missing documentation signals.

### 4. Finding

Findings are assessed conditions derived from observations.

Every finding must trace to observations. A finding may represent an issue,
risk, gap, positive architecture signal, or healthcheck result. Discovery
specific DTOs remain boundary objects; canonical reasoning uses AIM findings.

### 5. Recommendation

Recommendations are proposed actions derived from findings.

Every recommendation must trace to findings and carry rationale, effort, impact,
confidence, and lifecycle status. Recommendations are candidates for review,
not executable graph mutations.

### 6. Proposed Change

Recommendations can produce ProposedArchitectureChanges.

A proposed change must include workspace id, correlation id, recommendation id,
finding ids, evidence ids, and a proposed graph mutation. Supported mutations
currently include element addition and relationship addition.

Proposed changes may be accepted, rejected, or deferred. Rejected and deferred
changes do not mutate the graph.

### 7. Review Board

The Review Board opens a governed session over recommendation candidates and
proposed changes.

Participants may include human architects, deterministic reviewers, automated
reviewer stubs, and future provider-backed reviewers. Votes support approval,
rejection, deferral, requests for more evidence, and approval with conditions.

The Review Board derives a decision recommendation. It does not automatically
apply proposed changes.

### 8. Explicit Acceptance

If governance recommends acceptance, an explicit caller accepts the proposed
change through ProposedChangeService.

This is the only point in the workflow where a proposed intelligence output can
become a graph mutation. Acceptance is separate from Review Board closure so
future APIs, MCP tools, and UI flows can preserve approval separation and
authorization checks.

### 9. Graph Mutation

Accepted proposed changes mutate ArchitectureKnowledgeGraph only through
validated graph application services.

Element additions use ArchitectureElementService. Relationship additions use
RelationshipService. Relationship validation remains enforced by the graph
service boundary.

### 10. Audit And Event Trace

Implemented workflow stages emit typed architecture events using the architecture
event envelope:

- WorkspaceCreated
- DiscoveryStarted
- DiscoveryCompleted
- ReviewRequested
- ReviewCompleted
- ElementAdded
- RelationshipAdded

Each typed event includes workspace id, causation id, correlation id, actor,
source, timestamp, audit relevance, mutation target, and payload. Events are
retained in the existing append-only hash-chained audit sink.

## Traceability Invariants

- every workspace-scoped workflow must carry workspace identity
- related workflow events should carry a correlation id
- every architecture event must include a causation id
- every observation must trace to evidence
- every finding must trace to observations
- every recommendation must trace to findings
- every proposed change must trace to recommendation, findings, and evidence
- every review board decision must trace to session inputs and votes
- only accepted proposed changes may mutate the graph
- all graph mutation must go through validated graph services
- audit chains must remain append-only and hash linked

## Boundaries

Discovery does not directly mutate the graph.

Review Board decisions do not directly mutate the graph.

AI reviewers and providers must not directly mutate the graph.

MCP tools must operate at the governed boundary: read graph state, create
review/proposal workflows, and invoke validated application services where
authorized.

Persistence, REST APIs, UI flows, live providers, and event sourcing remain
outside this workflow document. They must depend on the same evidence-first,
proposal-based kernel workflow when introduced.
