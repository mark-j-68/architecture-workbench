# Architecture OS Reference Architecture

Architecture Workbench is an Architecture Operating System: a governed runtime
for architecture knowledge, decision intelligence, evidence, projections,
reviews, discovery, generation, and external agent collaboration.

The operating system metaphor is intentional. The platform must provide a
stable kernel and governed service boundaries before applications, AI providers,
and external tools can safely extend it.

## Logical Layers

### Architecture Kernel

The Architecture Kernel owns the rules that make the platform coherent.

The kernel is responsible for:

- canonical lifecycle semantics
- invariant enforcement
- traceability rules
- event model ownership
- mutation boundaries
- audit relevance rules
- graph and intelligence model consistency

The kernel does not mean a single Java module. It is the stable architectural
contract that all runtime services, applications, providers, and MCP tools must
respect.

### Core Services

Core services implement governed use cases over the kernel contracts.

Core services may read and mutate platform state only through validated
application services and must emit immutable architecture events for significant
changes.

### Applications Layer

Applications are user-facing or agent-facing surfaces over the platform.

Applications include web UI panels, CLI workflows, IDE integrations, review
dashboards, and generated workbench experiences. Applications must not own
canonical architecture state and must not bypass service validation.

### Provider Layer

Providers are replaceable adapters for external capabilities.

Provider examples include AI model providers, repository scanners, cloud
inventory APIs, documentation importers, diagram parsers, and code generation
backends. Provider-specific behavior must not leak into the kernel.

## Major Components

### Architecture Knowledge Graph

The Architecture Knowledge Graph is the canonical runtime boundary for
architecture structure.

It owns graph elements, relationships, and architectural meaning. Event Storming,
C4, BPMN, DMN, OpenAPI, ADRs, reviews, healthchecks, and generated artefacts are
projections or interpretations of this graph.

### Architecture Intelligence Model

The Architecture Intelligence Model owns evidence-backed reasoning concepts:
evidence, observations, findings, concerns, recommendations, decisions, metrics,
trends, reviewers, and confidence.

It provides the explainability substrate for review, governance, healthchecks,
and AI-assisted reasoning.

### Architecture Event Bus

The Architecture Event Bus is the immutable sequence of architecture domain
events emitted by governed services.

It is not just a messaging mechanism. It is the semantic record of platform
state transitions and significant decisions. Events support auditability,
traceability, replay, projection refresh, and future workflow automation.

### Workspace Service

The Workspace Service owns workspace identity and workspace-scoped graph
boundaries.

A workspace represents one architecture initiative, product, system, domain, or
imported codebase. Every graph, review, discovery run, projection, and decision
must be attributable to a workspace.

### Evidence Service

The Evidence Service records and governs evidence from repositories, documents,
diagrams, APIs, infrastructure, runtime telemetry, human input, and AI reviewer
outputs.

Evidence must preserve provenance, confidence, references, supporting artefacts,
and privacy classification. Interpretation must remain separate from evidence.

### Decision Intelligence Service

The Decision Intelligence Service turns architectural recommendations into
testable hypotheses, experiments, outcomes, learnings, and reusable patterns.

It closes the loop between architecture intent and real delivery outcomes.

### Review Board Service

The Review Board Service coordinates architecture reviews across deterministic
validators, human reviewers, and provider-neutral AI reviewer plugins.

It records findings, disagreements, consensus recommendations, reviewer
confidence, and decision outcomes against the graph and intelligence model.

### Projection Service

The Projection Service generates typed views from the canonical graph and
intelligence model.

Supported projection families include Event Storming, C4, BPMN, DMN, OpenAPI,
ADRs, review board reports, healthcheck reports, and generated implementation
packs.

Projection edits are not direct state mutations. They become proposed graph or
intelligence changes that require validation and audit.

### Discovery Service

The Discovery Service ingests existing-system evidence from repositories,
documentation, diagrams, APIs, infrastructure, and runtime systems.

Discovery records evidence first. It may propose graph elements,
relationships, findings, risks, and healthcheck results, but it must preserve
the distinction between observed evidence and inferred architecture.

### Generation Service

The Generation Service creates delivery artefacts from governed architecture
state.

Generated diagrams, ADRs, BPMN, DMN, OpenAPI, code skeletons, tests,
infrastructure, and agent instructions must remain traceable to graph elements,
decisions, recommendations, and evidence.

### MCP Boundary

The MCP Boundary is the external tool boundary for agents, IDEs, automation, and
provider-side workflows.

MCP tools may retrieve context, run validation, generate projections, submit
proposals, retrieve evidence, and inspect audit traces. MCP tools must not expose
uncontrolled direct mutation of canonical state.

### Provider Layer

The Provider Layer contains adapters for Claude, OpenAI/Codex, Gemini, local
models, repository hosts, cloud providers, document stores, scanners, and future
external services.

Provider adapters translate external capabilities into platform-neutral service
requests and events.

### Applications Layer

The Applications Layer contains product experiences built on top of services.

Examples include Design Mode, Discovery Mode, Architecture Review Board,
healthcheck dashboards, graph explorers, projection editors, ADR workbenches,
and generation workflows.

Applications orchestrate user intent. They do not own the platform's canonical
architecture knowledge.

## Kernel vs Services vs Applications vs Providers

The kernel defines what is valid.

Services perform governed work.

Applications expose workflows to people and agents.

Providers supply external capability behind stable contracts.

This separation prevents UI features, live AI integrations, MCP tools, or
provider-specific behavior from becoming hidden sources of architectural truth.
