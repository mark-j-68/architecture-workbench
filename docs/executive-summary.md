# Executive Summary — Architecture Workbench

**Architecture Workbench** is an AI-native architecture platform that transforms early business/domain knowledge and existing-system evidence into a governed architecture knowledge graph, validated projections, generated implementation guidance, and eventually a locally deployable software system.

The initial inspiration was a lightweight React prototype that accepted an image of an Event Storming session and used Claude Vision to extract a YAML domain model. That idea is now expanded into a full architecture lifecycle platform.

## Product Thesis

Modern AI coding tools are good at generating code, but weak at maintaining architectural intent. Architecture Workbench addresses that gap by making the architecture knowledge graph the primary source of truth.

From that graph, the platform can generate and maintain:

- Event Storming projections
- React Flow architecture graph projections
- C4 diagrams
- ADRs
- OpenAPI specifications
- AsyncAPI specifications
- BPMN and DMN artefacts
- Spring Boot service scaffolds
- Infrastructure templates
- Claude Code AGENTS.md files
- Validation reports
- Existing-system healthcheck reports
- Architecture review findings
- LocalStack deployment assets

## Differentiator

Architecture Workbench is not primarily a code generator. It is an architecture integrity tool.

Its goal is to prevent drift between business understanding, domain models, architecture decisions, generated code, infrastructure, and documentation.

The platform has two product modes:

- **Design Mode** for creating and evolving target architecture.
- **Discovery Mode** for understanding existing systems, running healthchecks, and tracing remediation decisions to evidence.

## M3 Direction

M3 prepares the AI-native platform foundation:

1. Architecture Knowledge Graph as the platform core.
2. Design Mode and Discovery Mode as first-class workflows.
3. Existing-system healthchecks represented as graph-backed risks, evidence, decisions, and review records.
4. MCP Agent Collaboration Layer as a controlled boundary.
5. AI Architecture Review Board as the governed AI review surface.
6. Immutable audit and traceability for prompts, responses, tools, evidence, and decision outcomes.

M3 does not implement live AI provider calls. It establishes the architecture model, documentation, and module boundaries needed before provider execution is added.

## North Star

A user should be able to design a target system or discover an existing one, build a governed architecture knowledge graph, validate and healthcheck it, review material decisions through the AI Architecture Review Board, generate artefacts, build code through governed AI agents, and deploy the initial system locally.
