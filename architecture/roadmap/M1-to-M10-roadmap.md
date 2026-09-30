# Architecture Workbench Roadmap

## Current release roadmap

| Release | Theme | Status |
|---|---|---|
| v0.1 | Foundation | Complete |
| v0.2 | Discovery Foundations | Complete |
| v0.3 | Product Architecture Intelligence | Complete |
| v0.4 | Architecture Semantics and Drift Intelligence | Designed |
| v0.5 | Multi-Agent Architecture Review | Planned |
| v0.6 | Architectural Memory | Planned |
| v0.7 | Outcome and Decision Intelligence | Planned |
| v0.8 | Real-World Discovery and Continuous Architecture | Planned |
| v0.9 | Platformisation and Governance | Planned |
| v1.0 | Architecture Intelligence Platform | Planned |

Release details:

- [Release 0.3 Product Architecture Intelligence](../reference/RELEASE-0.3-MULTI-REPO-PRODUCT-ARCHITECTURE.md)
- [Release 0.4 Architecture Semantics and Drift Intelligence](../reference/RELEASE-0.4-ARCHITECTURE-SEMANTICS-AND-DRIFT.md)

The M1–M10 plan below is the earlier implementation roadmap. It remains useful as architectural history and as a capability inventory. Its milestone numbering does not define the current release sequence; several items were deliberately delivered, narrowed, or deferred through the v0.1–v0.3 release plans.

## Earlier M1–M10 implementation roadmap

## M1 — Architecture Foundation

Deliverables:

- BRD
- Solution Architecture Document
- Initial C4 DSL
- Initial Architecture Domain Model
- Initial ADRs
- Roadmap

## M2 — Canonical Architecture Model and Governance

Deliverables:

- Java ArchitectureModel
- YAML/JSON import/export
- Model versioning
- Initial graph projection

## M3 — AI-Native Architecture Platform Foundation

Deliverables:

- `architecture-knowledge-graph` module as the platform core boundary.
- Canonical architecture knowledge graph entities: domain, DDD, C4, decision, risk, review, ADR, and evidence nodes.
- Relationship model for containment, dependencies, DDD flow, decisions, risks, review findings, and evidence traceability.
- Validated application service boundaries for creating elements, linking elements, recording findings, tracing decisions, and generating projections.
- Immutable audit event contract for all graph mutations.
- Design Mode foundation for target architecture creation and governed change.
- Discovery Mode foundation for existing-system ingestion, healthchecks, evidence capture, and risk-backed remediation.
- Projection contracts for Event Storming, React Flow, C4, BPMN, DMN, ADRs, OpenAPI, and AI reviews.
- MCP Agent Collaboration Layer architecture, tool boundary, and traceability contract.
- AI Architecture Review Board model for multi-reviewer assessments, disagreements, consensus recommendations, ADR drafts, and decision outcomes.

Out of scope for M3 foundation:

- Live Claude/OpenAI/Codex provider calls.
- Autonomous agent mutation of the graph.
- Production persistence adapters.

## M4 — Workbench API and Persistence

Deliverables:

- Spring Boot API
- Workspace service
- Knowledge graph repository
- File workspace support
- PostgreSQL persistence
- Immutable audit store adapter

## M5 — React Workbench Shell

Deliverables:

- React + TypeScript UI
- Workspace explorer
- Monaco editor
- Validation panel
- Generated artefact tabs
- Activity log
- Design Mode workbench
- Discovery Mode workbench
- AI Architecture Review Board panel

## M6 — Validation and Healthcheck Engine

Deliverables:

- Rule interface
- Initial DDD rules
- Initial architecture rules
- Validation dashboard
- Finding severity model
- Existing-system healthcheck rules
- Risk and evidence traceability

## M7 — Projection and Generator Framework

Deliverables:

- Generator plugin API
- C4 generator
- ADR generator
- AGENTS.md generator
- OpenAPI/AsyncAPI skeleton generators
- BPMN/DMN projection generators

## M8 — MCP Agent Collaboration Layer

Deliverables:

- AI provider abstraction
- MCP server tools over the knowledge graph
- Controlled read/propose/review tool contracts
- Prompt/response/tool trace persistence
- Claude reviewer adapter
- OpenAI/Codex reviewer adapter
- Consensus comparison workflow
- Human approval gate for graph mutations

## M9 — AI Architecture Review and Discovery Automation

Deliverables:

- Prompt library
- Vision extraction
- Architecture review
- Fix suggestions
- Existing-system discovery ingestion
- Healthcheck evidence gathering
- Review Board workflow

## M10 — Version 1.0

Deliverables:

- Service-specific AGENTS.md
- Build tasks
- Review tasks
- Integration tasks
- Handoff prompts
- Docker Compose generator
- CloudFormation generator
- LocalStack verification scripts
- Deployment dashboard
- End-to-end Event Storming image to local deployment workflow
- End-to-end existing-system healthcheck workflow
- Validated architecture knowledge graph
- Generated documentation
- Generated service contracts
- Generated AI agent instructions
- Local running system scaffold
