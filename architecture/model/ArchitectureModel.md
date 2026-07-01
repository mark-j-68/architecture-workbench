# Architecture Knowledge Graph Model

M3 reframes the canonical architecture model as a governed architecture knowledge graph. The graph is deliberately independent of React, Spring MVC controllers, Claude prompts, C4, OpenAPI, AsyncAPI, BPMN, DMN, CloudFormation and generated code.

The existing M2 `ArchitectureModel` remains as a compatibility model while the platform migrates to the `architecture-knowledge-graph` module. New foundation work should treat the graph as the platform core.

## Principle

The architecture knowledge graph is the source of truth. All generated artefacts and review surfaces are projections.

```text
ArchitectureKnowledgeGraph
  -> Event Storming Board
  -> ValidationReport
  -> React Flow ArchitectureGraph
  -> C4 DSL
  -> OpenAPI / AsyncAPI
  -> BPMN / DMN
  -> ADRs
  -> AI Architecture Review Board
  -> Existing-System Healthcheck Report
  -> AGENTS.md
  -> Spring Boot scaffold
  -> LocalStack / CloudFormation
```

## Core Node Types

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
- ADR
- ArchitectureReview
- Evidence

## Core Relationship Types

- contains
- handled_by
- emits
- governed_by
- supports
- realized_by
- depends_on
- mitigates
- supersedes
- documented_by
- reviewed_by
- evidenced_by
- traces_to

## Product Modes

### Design Mode

Design Mode is used to create and evolve target architecture. Users capture Event Storming input, create architecture elements, link relationships, validate DDD consistency, generate projections, draft ADRs, and submit material decisions to the AI Architecture Review Board.

### Discovery Mode

Discovery Mode is used to understand an existing system. The platform ingests repository, runtime, API, infrastructure, documentation, and operational evidence into the graph, runs healthchecks, identifies risks, and traces remediation decisions to evidence.

## Mutation Boundary

All graph mutations must be routed through validated application services:

- `ArchitectureElementService`
- `RelationshipService`
- `DddConsistencyValidationService`
- `ProjectionService`
- `ArchitectureReviewService`
- `DecisionTraceabilityService`

Direct mutation by UI screens, MCP agents, provider adapters, or generators is not allowed. Agents may read graph context and propose changes, but application services validate and apply accepted changes.

Every accepted mutation emits an immutable audit event.

## Module Boundaries

```text
architecture-knowledge-graph/
  ArchitectureKnowledgeGraph.java
  ArchitectureElement.java
  Relationship.java
  * entity/value object classes
  * application services
  * validation report classes
  * projection contracts
  * immutable audit event log

workbench-core/
  Legacy M2 model and validation primitives retained during migration.
```

## Initial Consistency Rules

- `KG-DDD-001`: every aggregate must declare a root entity.
- `KG-DDD-002`: every aggregate should be contained by a bounded context.
- `KG-DDD-003`: every command must be handled by an aggregate.
- `KG-DDD-004`: every domain event should be emitted by an aggregate.

These are intentionally small but prove the extension model for the larger validation and healthcheck engine.

## Governance Requirements

Architecture Workbench treats AI output, healthcheck findings, ADRs, generated contracts, and graph changes as governed decisions. The graph must support:

- immutable activity log envelopes
- encrypted protected payload references for sensitive traces
- cryptographic shredding of protected payloads
- prompt, response, tool, reviewer, evidence, and decision outcome traceability
- human approval before material graph mutation

The key design rule is that immutable activity envelopes must not contain PII. Sensitive content is represented by encrypted payload references and can be made unreadable through cryptographic shredding.
