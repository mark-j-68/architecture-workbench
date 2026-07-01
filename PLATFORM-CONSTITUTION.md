# Platform Constitution — Architecture Workbench

Architecture Workbench is an AI Operating System for Software Architecture.

It is not a diagramming tool, a document generator, or a code generator with architecture features attached. It is a governed platform for discovering, designing, validating, reviewing, explaining, and generating software architecture from a shared architecture knowledge graph.

## Vision

Architecture Workbench exists to make architectural intent durable, inspectable, and executable.

Modern delivery teams increasingly use AI agents to write code, generate tests, produce infrastructure, and explain systems. Those agents need a reliable architectural operating context. Architecture Workbench provides that context by maintaining a governed knowledge graph of systems, domains, decisions, risks, evidence, policies, reviews, and implementation boundaries.

The long-term vision is a platform that can:

- discover existing architecture from repositories, documentation, diagrams, APIs, infrastructure, and runtime systems;
- model target architecture through human-guided Design Mode;
- validate and healthcheck architecture continuously;
- coordinate AI reviewers through provider-neutral plugins;
- generate useful delivery artefacts without losing traceability;
- preserve auditability, explainability, privacy, and regulatory controls.

## Product Philosophy

Architecture is not a static drawing. It is a living body of decisions, trade-offs, constraints, evidence, responsibilities, and implementation consequences.

Architecture Workbench therefore treats architecture as operational knowledge. Diagrams and documents are useful, but they are outputs of the platform rather than the platform's core state.

The product should help architects, engineers, platform teams, delivery leads, auditors, and AI agents work from the same governed source of truth.

The platform must support both:

- **Design Mode**: green-field or target-state architecture creation.
- **Discovery Mode**: reverse-engineering and healthchecking existing systems.

## Core Architectural Principles

1. Architecture is a governed knowledge graph, not a set of diagrams.
2. Event Storming, C4, BPMN, DMN, OpenAPI, ADRs, healthchecks, and generated code are projections of the same graph.
3. Design Mode supports green-field architecture creation.
4. Discovery Mode supports reverse-engineering and healthchecking existing systems.
5. AI reviewers are plugins, not hard-coded platform features.
6. Claude, OpenAI/Codex, Gemini, and future models must be provider-neutral.
7. All mutations must go through validated application services.
8. All significant decisions must be traceable to evidence.
9. MCP is the external tool boundary.
10. The platform must support auditability, explainability, GDPR-aware data handling, and cryptographic shredding.
11. The system should eventually support both discovery and generation.
12. Agents may propose changes, but governed services apply accepted changes.

## Architecture Kernel

The graph is the runtime boundary, but the Architecture Kernel owns the rules
that make the platform coherent.

The kernel defines the canonical meta-model, architecture event model,
traceability invariants, lifecycle semantics, mutation boundaries, and audit
relevance rules. Services, applications, MCP tools, discovery connectors,
generation workflows, provider adapters, and AI reviewers must operate through
these kernel semantics rather than inventing separate state models.

## Knowledge Graph Model

The architecture knowledge graph is the canonical platform state.

The graph is also the canonical runtime boundary. API modules, UI surfaces, MCP tools, discovery connectors, healthcheck engines, projection generators, review-board workflows, and AI reviewer integrations must depend on the graph boundary rather than the legacy M2 `ArchitectureModel`.

Core graph element types include:

- DomainEvent
- Command
- Aggregate
- BoundedContext
- Capability
- Policy
- Decision
- Risk
- System
- Container
- Component
- Relationship
- ADR
- ArchitectureReview
- Evidence

Relationships express architectural meaning, including:

- containment
- ownership
- dependency
- command handling
- event emission
- policy governance
- capability realization
- risk mitigation
- decision documentation
- review assessment
- evidence traceability

The graph must support both current-state and target-state architecture. Discovery Mode may add observed evidence and confidence metadata. Design Mode may add intended design decisions and planned implementation boundaries.

## Projection Model

Projections are views over the graph.

The following must be treated as projections, not independent sources of truth:

- Event Storming boards
- C4 diagrams
- BPMN processes
- DMN decision models
- OpenAPI and AsyncAPI contracts
- ADR documents
- architecture healthcheck reports
- AI Architecture Review Board records
- generated code skeletons
- generated tests
- generated infrastructure

Projection generators should preserve traceability back to graph elements, relationships, decisions, and evidence.

If a projection is edited, the edit should become a proposed graph change. The platform must validate and audit that change before it mutates canonical graph state.

## Plugin/Reviewer Architecture

AI reviewers are plugins.

Reviewer plugins may represent perspectives such as:

- DDD
- security
- cloud
- regulatory
- delivery
- operational resilience
- data architecture
- platform engineering
- cost

Reviewer plugins must use provider-neutral contracts. A reviewer may be backed by Claude, OpenAI/Codex, Gemini, local models, deterministic rules, or a human workflow. The rest of the platform must not depend on provider-specific APIs.

Reviewer output should include:

- findings
- severity
- confidence
- rationale
- evidence references
- recommended action
- disagreements with other reviewers
- consensus status

Review output is recorded through the AI Architecture Review Board and linked to the knowledge graph.

## Discovery Architecture

Discovery Mode reverse-engineers existing systems into graph-backed architecture knowledge.

Discovery sources may include:

- source repositories
- dependency manifests
- code structure
- README and architecture documents
- C4, UML, BPMN, and DMN diagrams
- OpenAPI and AsyncAPI definitions
- database schemas
- infrastructure as code
- cloud resources
- CI/CD pipelines
- logs, metrics, traces, and runtime configuration

Discovery must distinguish evidence from interpretation. Evidence is recorded as graph evidence. Interpretation becomes a proposed relationship, risk, decision, or review finding.

Healthchecks should evaluate the discovered graph for architectural risks such as missing ownership, undocumented dependencies, stale decisions, unmanaged PII, missing audit controls, weak API governance, unmitigated operational risk, and divergence between documented and observed architecture.

## Generation Architecture

Generation starts from the graph.

The platform should eventually generate:

- diagrams
- ADRs
- BPMN
- DMN
- OpenAPI
- AsyncAPI
- code skeletons
- tests
- infrastructure
- AGENTS.md files
- implementation tasks
- review packs

Generated artefacts must remain traceable to graph nodes, relationships, decisions, and evidence. Generation should not create hidden architecture. If generation identifies a missing architectural decision, it should create a proposed decision or review finding rather than silently inventing intent.

## MCP Strategy

MCP is the external tool boundary.

External AI agents, IDE agents, review agents, and automation tools should interact with Architecture Workbench through controlled MCP tools.

MCP tools may expose:

- graph context retrieval
- projection retrieval
- validation execution
- healthcheck execution
- review history retrieval
- proposal submission
- evidence lookup
- ADR draft generation
- audit trace lookup

MCP tools must not expose unrestricted graph mutation.

Accepted changes must pass through validated application services. Material changes require audit events and, where appropriate, human approval.

MCP implementations must be graph-centred. Legacy model-oriented MCP tools may exist only as migration compatibility surfaces and must not be used for new runtime work.

## Governance and Audit

The platform must be governed by default.

Governance requirements include:

- immutable audit events for significant actions;
- traceability of prompts, responses, tools used, findings, evidence, and decision outcomes;
- clear distinction between evidence, inference, proposal, decision, and generated output;
- reviewer confidence and disagreement tracking;
- review board records for significant AI-assisted decisions;
- deterministic validation before AI judgement where practical;
- human approval for material architectural change.

The audit model must avoid storing sensitive payloads directly in immutable envelopes. Sensitive payloads should be referenced through protected payload references.

## Security and Privacy

Architecture Workbench must support regulated environments.

Security and privacy requirements include:

- GDPR-aware data handling;
- encrypted sensitive payload storage;
- cryptographic shredding through key destruction or wrapped-key destruction;
- PII classification;
- access control around evidence and review records;
- minimal immutable envelope contents;
- explainable data lineage;
- provider isolation for AI calls;
- no direct frontend calls to AI providers;
- no direct agent mutation of canonical graph state.

The platform must be able to make sensitive content unreadable while retaining non-sensitive immutable audit evidence that an action occurred.

## Provider Neutrality

AI providers are interchangeable implementation details.

The platform must support Claude, OpenAI/Codex, Gemini, local models, and future models through provider-neutral interfaces.

Provider-neutrality requires:

- stable reviewer contracts;
- provider-specific adapters outside the graph core;
- model identifiers recorded in audit traces;
- prompt and response traceability;
- no provider-specific assumptions in domain model or application service contracts;
- deterministic stubs and rule-backed reviewers for offline testing.

## Extension Model

Architecture Workbench should be extensible through plugins and adapters.

Extension points include:

- reviewer plugins
- projection generators
- discovery connectors
- healthcheck rules
- validation rules
- evidence classifiers
- code generators
- infrastructure generators
- MCP tools
- persistence adapters
- provider adapters

Extensions must respect platform governance. They may propose graph changes but must not bypass validated application services, audit events, or approval gates.

## Long-Term Roadmap

The long-term roadmap is to evolve Architecture Workbench into an architecture operating system that supports the full lifecycle:

1. Capture domain knowledge through Event Storming and structured workshops.
2. Discover existing systems from repositories, documentation, diagrams, APIs, infrastructure, and runtime systems.
3. Build and maintain a governed architecture knowledge graph.
4. Run deterministic validation and architecture healthchecks.
5. Coordinate provider-neutral AI reviewers through the AI Architecture Review Board.
6. Record significant decisions, evidence, risks, and recommendations.
7. Generate diagrams, ADRs, BPMN, DMN, OpenAPI, code skeletons, tests, infrastructure, and agent instructions.
8. Feed governed architecture context to coding agents through MCP.
9. Detect drift between intended architecture, discovered architecture, generated artefacts, and runtime reality.
10. Support audit, explainability, privacy, and cryptographic shredding for regulated delivery.

This constitution is binding architectural guidance for future work. New modules, ADRs, features, and agent integrations should be evaluated against it.
