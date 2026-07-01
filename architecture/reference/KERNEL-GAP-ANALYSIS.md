# Architecture Kernel Gap Analysis

## Source of Truth

This assessment uses the following documents as binding guidance:

- `PLATFORM-CONSTITUTION.md`
- `architecture/reference/ARCHITECTURE-OS-REFERENCE-ARCHITECTURE.md`
- `architecture/reference/ARCHITECTURE-KERNEL-META-MODEL.md`
- `architecture/reference/ARCHITECTURE-EVENT-MODEL.md`
- `architecture/adr/ADR-020-architecture-kernel-and-event-model.md`

The review covers the current Java modules and documentation state. It does not
recommend adding providers, persistence, REST APIs, UI runtime behavior, or new
Maven modules in this pass.

## 1. Current Module Alignment

### Kernel-Aligned Modules

These modules contain kernel concepts or kernel-adjacent abstractions:

- `architecture-knowledge-graph`
  - Strong alignment with the graph as canonical runtime boundary.
  - Contains `ArchitectureKnowledgeGraph`, `ArchitectureElement`,
    `Relationship`, graph element types, relationship validation, projection
    DTOs, DDD validation, decision-to-evidence tracing, and review graph
    concepts.
  - Gap: it is graph-centred but not yet workspace-scoped at the graph object
    level, and its audit actions are not yet first-class architecture domain
    events.

- `architecture-intelligence`
  - Strong alignment with the Architecture Intelligence Model.
  - Contains `Evidence`, `Observation`, `Finding`, `Concern`,
    `Recommendation`, `DecisionOutcome`, `Metric`, `Trend`, `Reviewer`, and
    services for promotion and decision recording.
  - Gap: objects enforce traceability internally, but services do not emit
    architecture events and concepts are not workspace-scoped.

- `decision-intelligence`
  - Strong alignment with organisational learning.
  - Contains `Hypothesis`, `Experiment`, `Outcome`, `Learning`, `Pattern`, and
    services for hypothesis validation, experiments, learning, and reusable
    pattern derivation.
  - Gap: no architecture events are emitted for hypothesis, experiment,
    outcome, learning, or pattern lifecycle changes.

- `platform-audit`
  - Provides a shared immutable audit abstraction with hash chaining.
  - This is necessary kernel infrastructure, but not yet the Architecture Event
    Bus described by the design pack.
  - Gap: current `AuditEvent` has `scopeType`, `scopeId`, `action`,
    `subjectRef`, and `details`, but lacks kernel event fields such as event
    type, source, causation id, correlation id, workspace id, actor kind, audit
    relevance, mutation target, and protected payload references.

### Service Modules

These modules implement governed use cases over kernel concepts:

- `workspace-service`
  - Correctly owns workspace identity and in-memory workspace/graph repository
    boundaries.
  - Emits audit records for workspace and graph operations.
  - Gap: event names use service-oriented audit actions such as
    `WORKSPACE_GRAPH_IMPORTED` instead of the kernel event names such as
    `GraphImported`.

- `review-board`
  - Connects agent-collaboration results to graph concepts such as
    `ArchitectureReview`, `Decision`, `Risk`, and `Evidence`.
  - Belongs in Services, not Kernel, because it coordinates review workflows.
  - Gap: it currently bridges the graph and reviewer outputs, but not the full
    Architecture Intelligence Model or kernel event model.

- `discovery-engine`
  - Implements first Discovery Mode service behavior with local repository
    scanning and healthcheck findings.
  - Belongs in Services. `LocalRepositoryDiscoveryConnector` is an internal
    connector prototype, not yet a provider adapter.
  - Gap: discovered artifacts are mapped directly to graph nodes where
    reasonable, but the design pack requires stronger separation between
    evidence, observation, interpretation, proposed graph changes, and accepted
    graph mutations.

- `agent-collaboration`
  - Implements provider-neutral reviewer contracts and deterministic stubs.
  - Best classified as a Service boundary for review coordination plus an early
    plugin contract.
  - Gap: the module uses its own `ReviewerType`, `ReviewFinding`, and
    `ReviewConsensus` concepts that overlap with Architecture Intelligence and
    Review Board semantics.

### Applications Layer

Current application-layer code is limited and mostly legacy:

- `workbench-ui`
  - Present in the repository but not part of the current Maven reactor.
  - It belongs in Applications when revived.

- `workbench-core/src/main/java/.../core/api`
  - Contains legacy API/controller code.
  - It belongs in Applications or API adapter space, but the whole
    `workbench-core` module is now marked as legacy compatibility.

No new application runtime should be added until the kernel event and service
contracts are stabilized.

### Provider Layer

There is no real Provider Layer yet.

Current provider-like code:

- `agent-collaboration` reviewer stubs are deterministic test doubles, not
  live AI providers.
- `discovery-engine/LocalRepositoryDiscoveryConnector` is a local connector
  implementation, not an external provider adapter.
- `workbench-core` contains deterministic Claude/OpenAI/Codex-named reviewer
  clients, but those are legacy compatibility fixtures rather than provider
  integrations.

This is acceptable for the current milestone because the design pack explicitly
pauses provider work.

### MCP Boundary

- `knowledge-graph-mcp`
  - Correctly belongs at the MCP Boundary.
  - It depends on the graph boundary and exposes controlled read/proposal tools.
  - Gap: MCP invocations are not yet represented as `McpToolInvoked` kernel
    events with causation/correlation and audit relevance.

### Legacy Compatibility

- `workbench-core`
  - Correctly marked as legacy M2 compatibility in package-level documentation.
  - Contains the old sectioned `ArchitectureModel`, early graph builder,
    validation rules, audit envelope, protected payload store, MCP server, API
    controller, and deterministic reviewer clients.
  - It should remain isolated and should not be used as the source for new
    runtime behavior.

## 2. Missing Kernel Concepts

### Missing Domain Types

The codebase has most kernel nouns, but several are incomplete or split:

- `Workspace` exists, but `ArchitectureKnowledgeGraph` itself does not carry a
  `WorkspaceId`.
- `ArchitectureGraph` is represented as `ArchitectureKnowledgeGraph`; the
  naming is acceptable but should be explicitly mapped to the kernel term.
- `Projection` exists in the graph module, but there is no workspace-scoped
  projection identity or lifecycle.
- `Review` is split across graph `ArchitectureReview`, agent collaboration
  `ReviewRequest` and `ReviewResponse`, and review-board records.
- `Evidence` exists in both `architecture-knowledge-graph` and
  `architecture-intelligence`.
- `Finding` exists as graph validation findings, discovery findings, agent
  review findings, and AIM findings.
- `ReviewerType` exists in both `agent-collaboration` and
  `architecture-intelligence`.
- There is no explicit `ArchitectureEvent` domain type.
- There is no explicit `ArchitectureEventEnvelope`.
- There is no explicit `MutationTarget` concept.
- There is no explicit `AuditRelevance` concept.
- There is no explicit `CausationId` or `CorrelationId` value object.
- There is no protected payload reference in the shared `platform-audit`
  abstraction, although legacy `workbench-core` contains protected payload
  concepts.

### Missing Event Types

The event model defines these semantic events, none of which exist as first
class event types yet:

- `WorkspaceCreated`
- `GraphImported`
- `ElementAdded`
- `RelationshipAdded`
- `EvidenceRecorded`
- `ObservationRecorded`
- `FindingCreated`
- `ConcernDefined`
- `HypothesisCreated`
- `RecommendationProposed`
- `DecisionRecorded`
- `ExperimentStarted`
- `OutcomeRecorded`
- `LearningDerived`
- `PatternPublished`
- `ReviewRequested`
- `ReviewCompleted`
- `ProjectionGenerated`
- `DiscoveryStarted`
- `DiscoveryCompleted`
- `HealthcheckCompleted`
- `ProviderInvoked`
- `McpToolInvoked`

Current modules emit string actions through audit logs, but there is no typed
architecture event taxonomy.

### Missing Invariants

The implementation currently enforces some local invariants:

- relationships must connect existing graph elements
- some relationship types validate source/target element types
- AIM objects require traceability to upstream objects
- decision intelligence objects require supporting evidence, recommendations,
  experiments, or learnings as appropriate
- audit logs are hash-chained

Missing kernel-level invariants:

- every graph, review, projection, discovery run, and architecture event must be
  workspace-scoped
- every significant mutation must emit a named architecture domain event
- every domain event must include causation id and correlation id
- events must state whether they mutate graph, intelligence model, both, or
  neither
- projection edits must become proposed changes rather than direct mutations
- evidence and interpretation must be represented as separate event and model
  concepts
- provider and MCP activity must never imply direct canonical state mutation
- sensitive payloads must be referenced from the shared audit/event layer

### Missing Lifecycle Semantics

Current lifecycle support is uneven:

- AIM recommendations have `LifecycleStatus`.
- AIM decisions have `DecisionStatus`.
- Decision intelligence hypotheses have `HypothesisStatus`.
- ADRs have status inside the graph module.

Missing lifecycle consistency:

- no shared lifecycle vocabulary across graph, AIM, review, discovery,
  projection, and decision intelligence
- no lifecycle transition events
- no lifecycle validation rules for accepted, rejected, deferred, active,
  superseded, or retired states
- no workspace-level lifecycle for imported, current, target, or archived graph
  snapshots

### Missing Traceability Links

Object-level traceability exists in AIM and decision intelligence, but not yet
across the whole kernel:

- graph elements can be linked to evidence through relationships, but the graph
  evidence type is separate from AIM evidence
- discovery artifacts do not become AIM evidence first
- healthcheck findings are discovery-specific, not AIM findings
- review findings are agent-specific, not AIM findings
- review-board records connect to graph concepts but not to the full
  AIM/decision-intelligence chain
- audit events do not yet carry enough causation/correlation data to reconstruct
  end-to-end workflows

## 3. Event Model Implementation Gap

### Current Audit Actions and Event Correspondence

Current audit records partially correspond to the kernel event model:

| Current audit action | Current module | Kernel event correspondence | Gap |
| --- | --- | --- | --- |
| `WORKSPACE_CREATED` | `workspace-service` | `WorkspaceCreated` | Rename/elevate to typed event with source, causation id, correlation id, workspace id, mutation target |
| `WORKSPACE_GRAPH_IMPORTED` | `workspace-service` | `GraphImported` | Rename/elevate; include snapshot id/import mode/source reference |
| `WORKSPACE_GRAPH_SAVED` | `workspace-service` | No direct event, possibly graph snapshot persisted | Needs decision: internal repository event or non-kernel audit event |
| `WORKSPACE_GRAPH_EXPORTED` | `workspace-service` | No direct event, possibly projection/export event | Needs decision: audit-only export event or `ProjectionGenerated` if exported view is a projection |
| `WORKSPACE_RENAMED` | `workspace-service` | Not in kernel event list | Add to event model later or keep as audit-only workspace metadata event |
| `ARCHITECTURE_ELEMENT_CREATED` | `architecture-knowledge-graph` | `ElementAdded` | Rename/elevate; include lifecycle state, evidence refs, validation result |
| `ARCHITECTURE_ELEMENTS_LINKED` | `architecture-knowledge-graph` | `RelationshipAdded` | Rename/elevate; include semantic relationship payload and validation result |
| `ARCHITECTURE_PROJECTION_GENERATED` | `architecture-knowledge-graph` | `ProjectionGenerated` | Rename/elevate; include projection id, graph snapshot id, output reference |
| `ARCHITECTURE_REVIEW_FINDING_RECORDED` | `architecture-knowledge-graph` | `FindingCreated` or `ReviewCompleted` | Needs separation between review completion and AIM finding creation |
| `DECISION_TRACED_TO_EVIDENCE` | `architecture-knowledge-graph` | No direct event; related to `DecisionRecorded` or traceability link event | Keep as audit-only or introduce typed traceability-link event later |
| `ARCHITECTURE_REVIEW_RUN_STARTED` | `agent-collaboration` | `ReviewRequested` | Rename/elevate; clarify whether request or execution start |
| `ARCHITECTURE_REVIEWER_STUB_RUN` | `agent-collaboration` | Could become `ProviderInvoked` only for real providers; otherwise reviewer assessment event | Do not map to `ProviderInvoked` while stubs are local |
| `ARCHITECTURE_REVIEW_CONSENSUS_GENERATED` | `agent-collaboration` | `ReviewCompleted` | Rename/elevate; include disagreement summary, finding ids, consensus recommendation |
| `ARCHITECTURE_REVIEW_RUN_COMPLETED` | `agent-collaboration` | `ReviewCompleted` | Duplicate with consensus event; consolidate |
| `DISCOVERY_RUN_STARTED` | `discovery-engine` | `DiscoveryStarted` | Rename/elevate; include connector/configuration references |
| `DISCOVERY_ARTIFACT_DISCOVERED` | `discovery-engine` | Potentially `EvidenceRecorded` or discovery-specific artifact event | Prefer record evidence first, then observations/findings |
| `DISCOVERY_RUN_COMPLETED` | `discovery-engine` | `DiscoveryCompleted` | Rename/elevate; include artifact count, proposed changes, finding ids, status |
| `HEALTHCHECK_FINDING_CREATED` | `discovery-engine` | `FindingCreated` and/or `HealthcheckCompleted` | Current event is per finding; kernel also needs healthcheck completion event |

### Missing Events

No current audit actions correspond to:

- `EvidenceRecorded` as a shared AIM event
- `ObservationRecorded`
- `ConcernDefined`
- `HypothesisCreated`
- `RecommendationProposed`
- `DecisionRecorded`
- `ExperimentStarted`
- `OutcomeRecorded`
- `LearningDerived`
- `PatternPublished`
- `ProviderInvoked`
- `McpToolInvoked`
- `HealthcheckCompleted` as a completion event

### Events to Rename or Elevate

The following should become typed architecture events rather than free-form
audit action strings:

- `WORKSPACE_CREATED` -> `WorkspaceCreated`
- `WORKSPACE_GRAPH_IMPORTED` -> `GraphImported`
- `ARCHITECTURE_ELEMENT_CREATED` -> `ElementAdded`
- `ARCHITECTURE_ELEMENTS_LINKED` -> `RelationshipAdded`
- `ARCHITECTURE_PROJECTION_GENERATED` -> `ProjectionGenerated`
- `DISCOVERY_RUN_STARTED` -> `DiscoveryStarted`
- `DISCOVERY_RUN_COMPLETED` -> `DiscoveryCompleted`
- `ARCHITECTURE_REVIEW_RUN_STARTED` -> `ReviewRequested`
- `ARCHITECTURE_REVIEW_RUN_COMPLETED` and
  `ARCHITECTURE_REVIEW_CONSENSUS_GENERATED` -> one coherent `ReviewCompleted`
  event

The following should remain audit-only unless the event model is deliberately
expanded:

- `WORKSPACE_RENAMED`
- `WORKSPACE_GRAPH_SAVED`
- `WORKSPACE_GRAPH_EXPORTED`
- `DECISION_TRACED_TO_EVIDENCE`
- `ARCHITECTURE_REVIEWER_STUB_RUN`

### Event Sourcing Recommendation

Do not introduce event sourcing now.

The immediate need is a typed architecture event envelope and semantic event
taxonomy that can be emitted alongside the existing in-memory audit sinks. Full
event sourcing would prematurely force replay, versioning, snapshotting,
upcasters, ordering guarantees, idempotency, and persistence concerns before
the kernel contracts are stable.

Recommended stance:

- Now: introduce typed architecture events and map them to the immutable audit
  abstraction.
- Later: introduce durable event storage only after workspace, graph,
  intelligence, and decision lifecycle contracts stabilize.
- Deferred: make event sourcing a persistence strategy decision, not a domain
  modeling shortcut.

## 4. Refactoring Recommendations

### Module Naming

Current module names are mostly acceptable for M4:

- `architecture-knowledge-graph` maps to the graph runtime boundary.
- `architecture-intelligence` maps to AIM.
- `decision-intelligence` maps to organisational learning.
- `workspace-service`, `discovery-engine`, `review-board`, and
  `knowledge-graph-mcp` map cleanly to service or boundary components.

Naming issues:

- `platform-audit` should either remain a low-level audit sink or evolve into a
  broader kernel event/audit contract. Do not silently turn it into an event bus
  without renaming contracts inside it.
- `agent-collaboration` is a reasonable current name, but its long-term role
  should be narrowed to reviewer plugin contracts and collaboration workflows.
- `workbench-core` must keep the legacy name and deprecation stance; avoid
  giving it new authority by renaming it to something kernel-like.

### Package Boundaries

Recommended boundary cleanup:

- Keep graph structural concepts in `architecture-knowledge-graph`.
- Keep evidence-backed reasoning in `architecture-intelligence`.
- Keep hypothesis-to-learning concepts in `decision-intelligence`.
- Keep workspace repositories and workspace operations in `workspace-service`.
- Move shared event envelope semantics into the shared audit/kernel boundary
  when implemented.
- Avoid putting provider, API, or UI concepts in graph, AIM, or decision
  intelligence packages.

### Dependency Direction

Current dependency direction is broadly acceptable:

- service modules depend on graph and audit
- decision intelligence depends on AIM
- review board depends on graph and agent collaboration
- MCP depends on graph

Dependency gaps:

- `architecture-intelligence` and `decision-intelligence` do not depend on a
  shared event/audit abstraction, so they cannot emit kernel events.
- `review-board` does not depend on AIM, so review outputs cannot yet become
  AIM findings, recommendations, or decisions without translation.
- `discovery-engine` depends directly on graph and audit but not AIM, so
  discovered evidence and healthcheck interpretation bypass the AIM chain.

### Duplicate Concepts

Duplicates to consolidate by mapping, not by immediate deletion:

- `Evidence`
  - graph evidence is an architecture element
  - AIM evidence is the evidence-backed reasoning object
  - Recommendation: establish AIM evidence as the reasoning source and graph
    evidence elements as references/projections into the graph.

- `Finding`
  - AIM finding, discovery finding, validation finding, review finding
  - Recommendation: keep specialized DTOs at boundaries, but normalize governed
    findings into AIM `Finding`.

- `ReviewerType`
  - appears in `agent-collaboration` and `architecture-intelligence`
  - Recommendation: define one canonical reviewer taxonomy or an explicit
    adapter mapping.

- `Review`
  - graph `ArchitectureReview`, agent review request/response, review-board
    records
  - Recommendation: review-board should own workflow records; graph should
    reference reviews; AIM should own findings/recommendations/decisions.

- audit concepts
  - legacy `workbench-core` has `ActivityEnvelope` and protected payload
    references
  - `platform-audit` has hash-chained `AuditEvent`
  - graph and agent modules wrap audit events separately
  - Recommendation: reuse the shared audit abstraction only; legacy audit
    concepts should not be used by new modules except as migration reference.

### Legacy Model Isolation

`workbench-core` is correctly documented as legacy compatibility. The next
boundary cleanup should ensure:

- no new module depends on `workbench-core`
- no new architecture event, provider, MCP, persistence, or API work is added to
  `workbench-core`
- legacy API/MCP/reviewer classes are treated as migration examples only
- any useful concepts such as protected payload references are reintroduced
  through shared kernel/audit contracts, not imported from `workbench-core`

## 5. Recommended Next Implementation Milestone

### M4.4: Architecture Event Envelope and Kernel Event Taxonomy

Implement one focused milestone: establish typed architecture domain events
without introducing event sourcing, persistence, providers, REST APIs, UI, or new
Maven modules.

Scope:

- Add a shared architecture event envelope to the existing shared audit/kernel
  boundary.
- Define value objects/enums for event type, source, causation id, correlation
  id, workspace id, audit relevance, and mutation target.
- Add typed event records for the current implemented actions:
  `WorkspaceCreated`, `GraphImported`, `ElementAdded`, `RelationshipAdded`,
  `ProjectionGenerated`, `DiscoveryStarted`, `DiscoveryCompleted`,
  `ReviewRequested`, and `ReviewCompleted`.
- Keep existing in-memory audit sinks.
- Map typed events into immutable audit records.
- Add event emission to existing services only where actions already happen.
- Add tests that prove event names, workspace scope, causation/correlation,
  mutation target, and hash-chained audit retention are present.

Constraints:

- No event sourcing.
- No database or file persistence.
- No live AI providers.
- No REST APIs.
- No UI.
- No new Maven modules unless a later ADR explicitly approves a kernel module
  split.
- No dependency on `workbench-core`.

Success criteria:

- Current free-form audit action strings are replaced or wrapped by typed
  architecture event types for implemented workflows.
- Every emitted architecture event has workspace id, source, causation id,
  correlation id, actor, audit relevance, timestamp, and mutation target.
- Existing tests remain green and new tests cover the event envelope.
- The implementation still uses the existing in-memory audit infrastructure.
- The event model remains a domain/audit contract, not an event-sourced
  persistence mechanism.

## 6. Risks

### Over-Engineering Risks

The kernel design pack is intentionally broad. Implementing every kernel concept
as a separate module, aggregate, repository, workflow, and event immediately
would slow progress and create abstractions before usage patterns are known.

Mitigation:

- implement the event envelope first
- map only events for workflows that already exist
- defer broad lifecycle engines, rule engines, and orchestration frameworks

### Premature Event Sourcing Risks

Event sourcing would add replay rules, snapshot policies, idempotency,
upcasting, ordering, storage guarantees, and operational semantics before the
kernel event contracts are proven.

Mitigation:

- treat architecture events as immutable audit/domain events now
- defer event sourcing until persistence and replay requirements are concrete
- do not let event sourcing drive the domain model

### Persistence Risks

Introducing Neo4j, PostgreSQL, files-on-disk, JPA, or Spring Data now would
freeze incomplete graph, workspace, event, and intelligence contracts too early.

Mitigation:

- keep in-memory repositories
- define stable repository and event contracts first
- choose persistence after workspace and event semantics are exercised by tests

### AI Provider Coupling Risks

Provider-specific concepts could leak into reviewer contracts, event payloads,
audit metadata, or graph state if live providers are introduced before the
kernel boundary is stable.

Mitigation:

- keep provider calls out of the next milestone
- record provider identity only through provider-neutral fields when
  `ProviderInvoked` is eventually implemented
- keep prompts, responses, tools used, and model identifiers behind protected
  payload references and audit/event metadata

## Summary

The codebase is directionally aligned with the Architecture Kernel Design Pack.
The graph, AIM, decision intelligence, workspace boundary, discovery service,
review-board service, MCP boundary, and shared audit abstraction are all present
at early maturity.

The main architectural gap is not another feature module. It is the absence of a
typed Architecture Event Bus contract that turns today’s free-form audit actions
into kernel-governed domain events with causation, correlation, workspace scope,
mutation target, and audit relevance.

The next implementation should therefore focus on the architecture event
envelope and typed event taxonomy before adding providers, APIs, UI, persistence,
or deeper generation/discovery behavior.
