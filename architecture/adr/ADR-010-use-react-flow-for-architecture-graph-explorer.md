# ADR-010 — Use React Flow for Architecture Graph Explorer

## Status
Accepted

## Context
Architecture Workbench needs two different visual surfaces:

1. A freeform Event Storming canvas for workshop capture.
2. A structured architecture graph explorer for navigating relationships derived from the canonical architecture knowledge graph.

tldraw is appropriate for freeform drawing and sticky-note capture. The architecture graph, however, needs nodes, edges, minimap navigation, filtering, selection, dependency traversal and later impact analysis.

## Decision
Use React Flow, now published as `@xyflow/react`, for the Architecture Graph Explorer in the React UI.

The backend will deterministically project the `ArchitectureKnowledgeGraph` into a React Flow view model. The frontend will render that projection with typed nodes and labelled edges.

The graph explorer is read-first. Editing the architecture graph directly is out of scope for the first version. Changes must be made through validated application services and then projected into React Flow.

## Consequences

Positive:

- Clear separation between freeform capture and structured model navigation.
- Built-in panning, zooming, minimap, controls and selection.
- Supports future graph features such as impact analysis, dependency tracing, bounded-context filtering and architectural risk visualisation.
- Keeps AI review grounded in explicit model relationships rather than inferred diagrams.

Negative:

- Adds another UI dependency.
- Requires a deterministic knowledge-graph-to-view projection layer.
- The graph layout will need refinement for large enterprise models.

## Initial Graph Node Types

- BOUNDED_CONTEXT
- AGGREGATE
- COMMAND
- DOMAIN_EVENT
- POLICY
- READ_MODEL
- SERVICE
- EXTERNAL_SYSTEM
- DEPLOYMENT_RESOURCE
- AI_JUDGE
- AUDIT_LOG

## Initial Relationship Types

- contains
- targets
- emits
- triggers
- issues
- populates
- owns
- publishes
- subscribes to
- uses
- records assessment
