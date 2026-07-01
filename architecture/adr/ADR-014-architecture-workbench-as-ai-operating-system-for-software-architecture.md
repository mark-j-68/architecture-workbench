# ADR-014 — Architecture Workbench as an AI Operating System for Software Architecture

## Status

Accepted

## Context

Architecture Workbench has evolved from an AI-assisted architecture modelling prototype into a broader platform concept.

Traditional modelling tools treat diagrams and documents as the main artefacts. That is not sufficient for AI-native software delivery. AI agents need structured, governed, evidence-linked architecture context. Architects need traceability, validation, review, healthchecks, and explainability. Delivery teams need projections and generated artefacts that stay aligned with architectural intent.

The platform now has two primary modes:

- Design Mode for green-field or target-state architecture creation.
- Discovery Mode for reverse-engineering and healthchecking existing systems.

The platform also needs provider-neutral reviewer plugins, MCP as the external tool boundary, immutable auditability, GDPR-aware data handling, and cryptographic shredding support.

## Decision

Treat Architecture Workbench as a graph-centred, AI-native architecture operating system rather than a traditional modelling tool.

The canonical platform state is the governed architecture knowledge graph. Event Storming, C4, BPMN, DMN, OpenAPI, ADRs, healthchecks, generated code, tests, infrastructure, and AI review records are projections of that graph.

AI reviewers are plugins. Claude, OpenAI/Codex, Gemini, local models, deterministic rules, and future models must integrate through provider-neutral contracts.

MCP is the external tool boundary. Agents may read context, run controlled tools, and submit proposals. They must not directly mutate the graph.

All mutations must go through validated application services and emit immutable audit events. Significant decisions must trace to evidence.

## Consequences

Positive:

- The product direction is clear: Architecture Workbench is a platform, not a diagram editor.
- AI agents receive governed architecture context rather than loose documents.
- Discovery, design, review, generation, and audit share one conceptual core.
- Provider neutrality is preserved.
- Regulated delivery concerns are designed into the platform rather than added later.

Negative:

- The platform scope is broader than a traditional modelling tool.
- The graph model, projection contracts, plugin contracts, and audit model must remain coherent as features expand.
- More upfront architectural discipline is required before adding live provider calls or autonomous agent workflows.

## Implementation Scope

This ADR is a documentation and architectural alignment milestone.

It does not implement new runtime functionality, live provider calls, new MCP tools, discovery connectors, or generation pipelines. Those capabilities must be added later in alignment with the platform constitution.

## Related Decisions

- ADR-011: Architecture Knowledge Graph as Platform Core
- ADR-012: Discovery Mode and Architecture Healthchecks
- ADR-013: MCP Agent Collaboration Layer
