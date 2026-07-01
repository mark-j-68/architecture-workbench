# ADR-001 — Use Architecture Model as the Source of Truth

## Status

Superseded by ADR-011

## Context

The prototype used YAML as the shared state between screens and prompts. This was effective for demonstration, but the platform needs a stronger foundation as it expands to validation, generation, graph analysis, AI review, and deployment.

If diagrams, documentation, infrastructure, AI instructions, and code scaffolds are generated from different prompts or documents, they will drift.

## Decision

Architecture Workbench will use a canonical `ArchitectureModel` as the primary source of truth.

YAML and JSON will be import/export formats, not the internal architecture.

All validators, generators, graph views, AI reviewers, and deployment plugins shall operate on the `ArchitectureModel`.

## M3 Update

This decision established the correct projection principle, but M3 reframes the platform around a richer `ArchitectureKnowledgeGraph`.

ADR-011 supersedes this ADR for new platform work:

- the architecture knowledge graph is the platform core;
- Event Storming, React Flow, C4, BPMN, DMN, ADRs, OpenAPI, AI reviews, and healthchecks are projections;
- graph mutations must be routed through validated application services;
- immutable audit events are emitted for all accepted changes.

## Consequences

Positive:

- Consistent generated artefacts
- Easier validation
- Clear traceability
- Easier graph generation
- Better AI grounding

Negative:

- Requires careful model design
- Up-front modelling effort is higher
- Migration from prototype YAML state is required
