# Architecture Event Model

The Architecture Event Model defines immutable domain events for the
Architecture OS kernel.

Events are emitted by governed services when significant architecture state,
intelligence state, review state, discovery state, projection state, provider
activity, or MCP activity occurs.

## Event Envelope

Every architecture event uses a common envelope:

- event id: globally unique identifier for the event
- event type: stable event name
- purpose: why the event exists
- payload: event-specific immutable data
- source: service, application, MCP tool, provider adapter, or workflow that
  emitted the event
- causation id: the command, request, tool call, or prior event that directly
  caused this event
- correlation id: identifier that groups related events in one workflow
- workspace id: workspace affected by the event
- timestamp: event creation time
- actor: human, service account, agent, reviewer, or system identity
- audit relevance: whether the event must be retained in the immutable audit log
- mutation target: graph, intelligence model, both, or neither

Sensitive payload data must not be embedded directly in immutable envelopes.
Sensitive content should be referenced by protected payload references that
support access control and cryptographic shredding.

## Events

### WorkspaceCreated

Purpose: records creation of a workspace boundary.

Payload: workspace id, name, description, owner, metadata.

Source: Workspace Service or onboarding workflow.

Causation id: create workspace command.

Correlation id: workspace onboarding workflow id.

Workspace id: created workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: neither graph nor intelligence model.

### GraphImported

Purpose: records import of an initial or replacement graph snapshot.

Payload: graph id, snapshot id, source reference, import mode, element count,
relationship count.

Source: Workspace Service, Discovery Service, import workflow, or MCP proposal
accepted by a governed service.

Causation id: import graph command.

Correlation id: import workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: graph.

### ElementAdded

Purpose: records creation of a graph element.

Payload: element id, element type, name, lifecycle state, evidence references,
validation result.

Source: Architecture Knowledge Graph service.

Causation id: create architecture element command.

Correlation id: design, discovery, review, or generation workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: graph.

### RelationshipAdded

Purpose: records creation of a relationship between graph elements.

Payload: relationship id, source element id, target element id, relationship
type, evidence references, validation result.

Source: Architecture Knowledge Graph service.

Causation id: link elements command.

Correlation id: design, discovery, review, or generation workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: graph.

### EvidenceRecorded

Purpose: records evidence captured for architecture reasoning.

Payload: evidence id, source, provenance, confidence, references, supporting
artifact references, privacy classification.

Source: Evidence Service, Discovery Service, Review Board Service, provider
adapter, or human workflow.

Causation id: record evidence command.

Correlation id: discovery, review, import, or design workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### ObservationRecorded

Purpose: records an observation derived from evidence.

Payload: observation id, source, description, evidence references, related graph
element ids.

Source: Architecture Intelligence Model service, Discovery Service, Review
Board Service, or healthcheck workflow.

Causation id: record observation command or prior evidence event id.

Correlation id: discovery, review, healthcheck, or analysis workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### FindingCreated

Purpose: records creation of an assessed finding.

Payload: finding id, severity, category, description, observation references,
confidence, related graph element ids.

Source: Architecture Intelligence Model service, Healthcheck Service, Review
Board Service, or deterministic validator.

Causation id: create finding command or observation event id.

Correlation id: review, healthcheck, validation, or discovery workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### ConcernDefined

Purpose: records definition of a durable architectural concern.

Payload: concern id, name, description, category.

Source: Architecture Intelligence Model service or governance workflow.

Causation id: define concern command.

Correlation id: governance workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### HypothesisCreated

Purpose: records creation of a testable architectural hypothesis.

Payload: hypothesis id, statement, rationale, evidence references,
recommendation references, confidence, status.

Source: Decision Intelligence Service.

Causation id: create hypothesis command.

Correlation id: review, decision, experiment, or learning workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### RecommendationProposed

Purpose: records a proposed architecture action.

Payload: recommendation id, description, rationale, related concern ids,
supporting finding ids, estimated impact, estimated effort, confidence,
lifecycle status.

Source: Architecture Intelligence Model service, Review Board Service,
Healthcheck Service, or Generation Service.

Causation id: propose recommendation command or finding event id.

Correlation id: review, healthcheck, discovery, or design workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### DecisionRecorded

Purpose: records acceptance, rejection, or deferral of a recommendation or
proposal.

Payload: decision id, decision status, rationale, reviewer ids, evidence
references, recommendation references.

Source: Review Board Service, governance workflow, or human approval workflow.

Causation id: record decision command.

Correlation id: review, governance, or change workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### ExperimentStarted

Purpose: records start of an experiment testing a hypothesis.

Payload: experiment id, hypothesis id, implementation date, expected outcomes,
metric ids.

Source: Decision Intelligence Service.

Causation id: record experiment command.

Correlation id: decision-learning workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### OutcomeRecorded

Purpose: records actual outcome of an experiment.

Payload: outcome id, experiment id, success level, observation references,
evidence references, actual outcomes.

Source: Decision Intelligence Service.

Causation id: compare expected and actual outcomes command.

Correlation id: decision-learning workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### LearningDerived

Purpose: records organisational learning derived from experiment outcomes.

Payload: learning id, summary, experiment references, confidence, pattern
references.

Source: Decision Intelligence Service.

Causation id: derive learning command.

Correlation id: decision-learning workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### PatternPublished

Purpose: records publication of a reusable architectural pattern.

Payload: pattern id, name, description, applicability, known tradeoffs, learning
references.

Source: Decision Intelligence Service or governance workflow.

Causation id: derive reusable pattern command.

Correlation id: decision-learning or governance workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### ReviewRequested

Purpose: records a request for architecture review.

Payload: review id, review type, requester, scope, reviewer types, graph
context references, prompt or policy reference.

Source: Review Board Service, UI workflow, MCP tool, or scheduled governance
workflow.

Causation id: request review command or MCP tool call id.

Correlation id: review workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: neither graph nor intelligence model.

### ReviewCompleted

Purpose: records completion of an architecture review.

Payload: review id, reviewer response references, finding ids, disagreement
summary, consensus recommendation, decision references, ADR draft reference.

Source: Review Board Service.

Causation id: review requested event id.

Correlation id: review workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### ProjectionGenerated

Purpose: records generation of a typed projection from graph or intelligence
state.

Payload: projection id, projection type, graph snapshot id, element references,
decision references, evidence references, output reference.

Source: Projection Service, UI workflow, MCP tool, or Generation Service.

Causation id: generate projection command or MCP tool call id.

Correlation id: projection, review, discovery, or generation workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required for governed projections; optional for transient
local previews.

Mutation target: neither graph nor intelligence model.

### DiscoveryStarted

Purpose: records start of existing-system discovery.

Payload: discovery run id, source type, source reference, connector id,
configuration reference.

Source: Discovery Service.

Causation id: start discovery command.

Correlation id: discovery workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: neither graph nor intelligence model.

### DiscoveryCompleted

Purpose: records completion of discovery.

Payload: discovery run id, artifact count, evidence references, proposed graph
change references, finding ids, status.

Source: Discovery Service.

Causation id: discovery started event id.

Correlation id: discovery workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: both when accepted discoveries create graph elements and
intelligence records; otherwise intelligence model only.

### HealthcheckCompleted

Purpose: records completion of an architecture healthcheck.

Payload: healthcheck id, scope, rule set id, finding ids, recommendation ids,
summary status.

Source: Healthcheck Service or Discovery Service.

Causation id: run healthcheck command.

Correlation id: healthcheck or discovery workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required.

Mutation target: intelligence model.

### ProviderInvoked

Purpose: records invocation of an external provider through a provider-neutral
adapter.

Payload: provider type, provider id, model or API version where applicable,
request reference, response reference, tool references, policy classification,
status.

Source: Provider Layer adapter.

Causation id: review, discovery, generation, or provider command id.

Correlation id: parent workflow id.

Workspace id: target workspace.

Timestamp: required.

Audit relevance: required when provider invocation influences architecture
state, decisions, reviews, or generated artefacts.

Mutation target: neither graph nor intelligence model directly.

### McpToolInvoked

Purpose: records invocation of a controlled MCP tool.

Payload: tool name, tool version, caller identity, input reference, output
reference, authorization decision, status.

Source: MCP Boundary.

Causation id: MCP request id.

Correlation id: parent agent, review, projection, discovery, or generation
workflow id.

Workspace id: target workspace where applicable.

Timestamp: required.

Audit relevance: required for governed tools and any tool that reads sensitive
context, proposes change, invokes providers, or produces review outputs.

Mutation target: neither graph nor intelligence model directly.

## Event Invariants

- Events are immutable once recorded.
- Event type names are stable public contracts.
- Every event that affects a workspace must include workspace id.
- Causation and correlation identifiers are mandatory for traceable workflows.
- Significant architecture decisions, provider invocations, review outputs, and
  MCP tool invocations are audit-relevant.
- Events may describe mutations, but only governed services perform mutations.
- Provider and MCP events must never imply direct canonical state mutation.
- Sensitive payloads must be referenced, not embedded, in immutable envelopes.
