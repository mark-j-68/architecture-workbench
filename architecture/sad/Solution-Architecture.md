# Solution Architecture Document — Architecture Workbench

## 1. Architecture Vision

Architecture Workbench is an AI-native architecture platform. Its central idea is that a governed architecture knowledge graph is the primary artefact. Event Storming boards, React Flow diagrams, C4, BPMN, DMN, ADRs, OpenAPI contracts, healthcheck reports, and AI reviews are projections from that graph.

The platform should evolve from a lightweight prototype into a modular architecture workbench with Design Mode, Discovery Mode, validation, healthchecks, review, generation, and deployment capabilities.

## 2. Architecture Principles

### Principle 1 — Knowledge Graph First

The canonical architecture knowledge graph is the source of truth. YAML, diagrams, documentation, infrastructure, AI review records, and generated code are views of that graph.

### Principle 2 — AI Assists, Humans Approve

AI may extract, refine, review, and generate artefacts, but human approval is required before applying material changes.

### Principle 3 — Deterministic Validation and Healthchecks Before AI Review

Rule-based validation and existing-system healthchecks should happen before AI review. AI should reason over validated graph data and cited evidence, not raw unstructured prompts alone.

### Principle 4 — Plugin-Based Generation

Generators for C4, OpenAPI, AsyncAPI, BPMN, DMN, CloudFormation, Docker, docs, and AGENTS.md are plugins.

### Principle 5 — Local-First

The workbench should be usable locally with Docker, LocalStack, and file-based workspace storage before cloud deployment.

### Principle 6 — Architecture Traceability

Generated outputs must be traceable to source model elements and architecture decisions.

### Principle 7 — No Direct Agent Mutation

MCP agents and AI reviewers may read context and propose changes. Accepted changes are applied only by validated application services and are recorded in the immutable audit log.

## 3. Target Logical Architecture

```text
React Workbench UI
        |
Spring Boot Workbench API
        |
Architecture Engine
        |
+-----------------------------+-------------------+-------------------+
| Knowledge Graph Service      | Validation Engine | Projection Engine |
+-----------------------------+-------------------+-------------------+
| MCP Collaboration Boundary   | Healthcheck Engine| Workspace Service |
+-----------------------------+-------------------+-------------------+
| AI Review Board Records      | Governance/Audit  | Generation Engine |
+-----------------------------+-------------------+-------------------+
        |
PostgreSQL / File Workspace / LocalStack / Git Providers
```

## 4. Major Components

### 4.1 Workbench UI

A React + TypeScript application providing:

- Workspace explorer
- Architecture model editor
- Validation panel
- AI review panel
- AI Architecture Review Board
- Generated artefact tabs
- Architecture graph view
- Design Mode
- Discovery Mode
- Existing-system healthchecks
- Activity log
- Build/deploy workflow screens

### 4.2 Workbench API

A Spring Boot backend exposing REST endpoints for:

- Workspace management
- Architecture knowledge graph import/export
- Design Mode graph mutation workflows
- Discovery Mode ingestion and healthchecks
- Validation execution
- Projection and generation execution
- AI review record orchestration
- MCP controlled-tool boundary
- Git integration
- LocalStack orchestration

### 4.3 Architecture Knowledge Graph Core

The `architecture-knowledge-graph` module contains:

- ArchitectureKnowledgeGraph aggregate
- ArchitectureElement and Relationship model
- DomainEvent, Command, Aggregate, BoundedContext, Capability, Policy
- Decision, Risk, ADR, ArchitectureReview, Evidence
- System, Container, Component
- DDD consistency validation
- Projection contracts
- Immutable audit events
- Application services for all mutations

This module has no AI provider dependencies and no MCP transport dependencies.

### 4.4 Validation Engine

Executes deterministic rules against the architecture knowledge graph.

Initial rule groups:

- DDD rules
- Naming rules
- Command/event rules
- Service boundary rules
- Context map rules
- API rules
- Messaging rules
- Security readiness rules
- Evidence traceability rules

### 4.5 Projection and Generation Engine

Creates projections from the architecture knowledge graph and loads generator plugins for material artefacts.

Initial projections/generators:

- Event Storming board
- React Flow graph
- C4 Structurizr DSL
- ADR Markdown
- AGENTS.md
- OpenAPI placeholder
- AsyncAPI placeholder
- BPMN placeholder
- DMN placeholder
- Docker Compose placeholder
- LocalStack placeholder

### 4.6 MCP Agent Collaboration Layer

Provides a controlled tool boundary over the graph. The MCP layer exposes read, validation, projection, review-record, and proposal tools. It must not expose direct graph mutation tools.

Capabilities:

- Retrieve graph context.
- Run validation and healthchecks.
- Generate projections.
- Record proposed review findings.
- Trace prompts, responses, tools used, evidence, and decision outcomes.
- Route accepted changes through application services.

Provider-specific Claude/OpenAI/Codex calls are deferred until after M3 foundation.

### 4.7 AI Architecture Review Board

Stores governed review records derived from the graph.

Review Board records include:

- Claude assessment
- OpenAI/Codex assessment
- disagreements
- consensus recommendation
- generated ADR draft
- linked risks
- linked evidence
- decision outcome
- immutable audit references

### 4.8 Discovery Mode and Healthchecks

Discovery Mode builds a graph-backed baseline of an existing system and runs healthchecks across source, runtime, contracts, infrastructure, documentation, and operational evidence.

Healthcheck findings are represented as Risk, Evidence, Decision, and ArchitectureReview graph elements rather than standalone reports.

### 4.9 Workspace Service

Manages persisted artefacts.

A workspace contains:

```text
architecture/
  knowledge-graph.yaml
  adr/
  c4/
  reviews/
  evidence/
  validation/
  healthchecks/
generated/
  openapi/
  asyncapi/
  bpmn/
  dmn/
  agents/
  infra/
```

## 5. Deployment View

Initial deployment is local-first.

```text
Developer Machine
  Docker Compose
    workbench-ui
    workbench-api
    postgres
    localstack
```

Later deployment can target AWS.

## 6. Data Architecture

M1 assumes file-based architecture artefacts. Later milestones may persist workspaces in PostgreSQL.

Recommended storage model:

- PostgreSQL for workspace metadata and model snapshots
- Graph repository for architecture elements and relationships
- File system / Git repository for generated artefacts
- S3-compatible storage for uploaded images and large artefacts

## 7. Security Architecture

Initial security controls:

- No AI/API tokens in generated artefacts
- Secrets passed by environment variables
- Human approval for Git push and generated changes
- Audit log for AI-generated changes
- Workspace-level access control later

## 8. Key Interfaces

### GeneratorPlugin

```java
public interface GeneratorPlugin {
    String id();
    String displayName();
    boolean supports(ArchitectureModel model);
    GenerationResult generate(ArchitectureModel model, GenerationContext context);
}
```

### ValidationRule

```java
public interface ValidationRule {
    String id();
    RuleSeverity defaultSeverity();
    List<ValidationFinding> validate(ArchitectureModel model);
}
```

### AIProvider

```java
public interface AIProvider {
    AIResponse complete(AIRequest request);
    AIResponse vision(VisionRequest request);
}
```

## 9. First Implementation Slice

The first implementation slice after M1 should deliver:

1. Spring Boot API with ArchitectureModel classes.
2. YAML import/export.
3. Initial validation rules.
4. React shell with model editor and validation panel.
5. C4 generator plugin.
6. ADR generator plugin.

## 10. Major Risks

| Risk | Impact | Mitigation |
|---|---:|---|
| AI hallucination | High | Validate AI output before accepting it |
| Model too complex too early | High | Start with a small canonical model and evolve |
| Generator sprawl | Medium | Plugin API and clear output contracts |
| UI becomes monolithic | Medium | Workspace/editor/plugin layout from start |
| Over-automation | Medium | Human approval workflow |

## 11. Architecture Decision Summary

The major initial decisions are captured as ADRs:

- ADR-001 — Architecture model as source of truth
- ADR-002 — Modular monolith first
- ADR-003 — Plugin-based generation framework
- ADR-004 — AI provider abstraction
- ADR-005 — Validation engine
