# Architecture Kernel Meta-Model

The Architecture Kernel meta-model defines the canonical concepts that the
platform uses to govern architecture state, intelligence, traceability, review,
learning, projection, and audit.

The meta-model is the stable semantic contract for services, applications, MCP
tools, providers, and future persistence adapters.

## Canonical Concepts

### Workspace

A Workspace represents one architecture initiative, product, system, domain, or
imported codebase.

Ownership:

- owns one or more architecture graph snapshots over time
- scopes reviews, discovery runs, projections, audit events, and decisions
- provides the top-level boundary for access control and lifecycle management

Invariant:

- every graph, review, projection, discovery run, and architecture event must be
  associated with a workspace

### ArchitectureGraph

An ArchitectureGraph is the canonical structure of architecture knowledge inside
a workspace.

Ownership:

- owns architecture elements
- owns relationships between elements
- references evidence, reviews, risks, decisions, and projections through stable
  identifiers

Invariant:

- graph mutation must occur through validated application services
- graph state must not be mutated directly by agents, UI components, providers,
  or projection editors

### ArchitectureElement

An ArchitectureElement is a typed node in the graph.

Examples include bounded contexts, capabilities, systems, containers,
components, commands, domain events, policies, ADRs, risks, reviews, and
evidence references.

Invariant:

- each element has stable identity, type, name, lifecycle state, and workspace
  scope

### Relationship

A Relationship expresses architectural meaning between elements.

Examples include contains, depends on, emits, handles, governs, realizes,
mitigates, documents, reviews, evidences, and supersedes.

Invariant:

- relationships must connect known graph elements
- relationship types must preserve semantic meaning and traceability

### Evidence

Evidence is observed or supplied material that supports architectural reasoning.

Examples include code files, commits, diagrams, API definitions, ADRs, cloud
resources, runtime telemetry, human statements, reviewer outputs, and imported
documents.

Invariant:

- evidence must include provenance, confidence, timestamp, references, and
  supporting artefacts
- interpretation must not be stored as evidence

### Observation

An Observation is a statement derived from evidence.

Invariant:

- every observation must trace back to one or more evidence records

### Finding

A Finding is an assessed issue, strength, risk, gap, or notable condition derived
from observations.

Invariant:

- every finding must trace back to one or more observations

### Concern

A Concern is a durable architectural topic requiring attention.

Examples include security, availability, latency, regulatory compliance,
maintainability, domain integrity, cloud cost, and delivery risk.

Invariant:

- concerns classify findings and recommendations without replacing evidence
  traceability

### Hypothesis

A Hypothesis is a testable architectural claim grounded in evidence and related
to recommendations.

Invariant:

- every hypothesis must trace to supporting evidence and related
  recommendations

### Recommendation

A Recommendation is a proposed action with rationale, effort, impact,
confidence, concerns, and supporting findings.

Invariant:

- every recommendation must trace back to findings

### DecisionOutcome

A DecisionOutcome records whether a recommendation or proposal was accepted,
rejected, or deferred, including rationale, reviewers, evidence, and timestamp.

Invariant:

- every decision must trace back to one or more recommendations and evidence

### Experiment

An Experiment tests a hypothesis through an implementation or controlled
architectural change.

Invariant:

- every experiment must trace back to a hypothesis
- expected outcomes must be captured before comparing actual outcomes

### Outcome

An Outcome records the result of an experiment, including success level,
observations, and supporting evidence.

Invariant:

- every outcome must trace back to observations and evidence

### Learning

A Learning captures reusable organisational knowledge derived from one or more
experiments.

Invariant:

- every learning must trace back to experiments
- every learning must be able to produce reusable architectural patterns

### Pattern

A Pattern is reusable architectural guidance derived from learnings.

Invariant:

- every pattern must trace back to one or more learnings

### Review

A Review records an architecture assessment requested by a user, workflow,
healthcheck, agent, or policy.

Invariant:

- every review must be workspace-scoped
- review findings, reviewer outputs, disagreements, consensus, and decisions
  must be traceable

### Reviewer

A Reviewer represents a human, deterministic rule engine, or automated AI-backed
review capability.

Invariant:

- reviewer identity, type, capabilities, provider where applicable, version, and
  human-or-automated classification must be explicit

### Projection

A Projection is a typed view generated from the graph and intelligence model.

Examples include Event Storming, React Flow, C4, BPMN, DMN, OpenAPI, ADR,
review board, healthcheck, and generated implementation views.

Invariant:

- projections are not canonical state
- projection edits become proposed changes subject to validation and audit

### AuditEvent

An AuditEvent is an immutable record that a significant action occurred.

Invariant:

- audit events must preserve actor, action, timestamp, workspace context,
  correlation identifiers, and non-sensitive immutable metadata
- sensitive payloads must be referenced through protected payload references

## Relationships

The kernel recognizes the following primary traceability chain:

`Evidence -> Observation -> Finding -> Hypothesis -> Recommendation -> DecisionOutcome -> Experiment -> Outcome -> Learning -> Pattern`

Graph structure and intelligence concepts are connected by references:

- graph elements may be supported by evidence
- observations may reference graph elements
- findings may identify graph risks, gaps, or strengths
- recommendations may propose graph changes
- decisions may accept or reject recommendations
- experiments may validate accepted architectural decisions
- learnings may publish reusable patterns
- reviews may produce findings, recommendations, decisions, or evidence
- projections may reference graph elements and intelligence records

## Ownership

Workspace owns lifecycle scope.

ArchitectureGraph owns canonical structure.

Architecture Intelligence owns evidence-backed reasoning.

Decision Intelligence owns hypothesis-to-learning semantics.

Review Board owns reviewer coordination and consensus records.

Projection Service owns generated views.

Audit owns immutable accountability records.

## Lifecycle

Concepts move through explicit lifecycle states where appropriate:

- proposed
- validated
- accepted
- rejected
- deferred
- active
- superseded
- retired

Lifecycle transitions must emit architecture domain events when they represent
significant platform state changes.

## Traceability

Traceability is mandatory, not optional.

The platform must be able to answer:

- what evidence supports this observation?
- what observations support this finding?
- what findings support this recommendation?
- what recommendations informed this decision?
- what hypothesis did this experiment test?
- what outcomes produced this learning?
- what learnings support this pattern?
- what events and actors changed the state?

## Kernel Invariants

- ArchitectureGraph is the canonical runtime boundary for architecture
  structure.
- The kernel owns rules, event semantics, traceability, and lifecycle behavior.
- Mutations must go through validated application services.
- Projections are generated views, not sources of truth.
- Evidence and interpretation must remain distinct.
- Significant decisions must be traceable to evidence.
- Reviews and AI outputs are governed records, not direct mutations.
- Audit events must be immutable and privacy-aware.
- Provider-specific concepts must not leak into the kernel.
