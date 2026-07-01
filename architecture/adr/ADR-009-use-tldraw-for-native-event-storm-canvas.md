# ADR-009 — Use tldraw for Native Event Storm Canvas

## Status

Accepted

## Context

Architecture Workbench originally supported Event Storming capture through image upload. That is useful for importing existing workshop material, but it forces the system to infer structure from pixels. A regulated architecture tool benefits from a native canvas where sticky notes, positions, relationships, provenance, and confidence can be captured as structured data.

## Decision

Use `tldraw` as the embedded React infinite canvas for native Event Storming sessions. Store canvas content as capture evidence and projection state linked to the canonical architecture knowledge graph.

Event Storming is not a separate source of truth. Sticky notes and links either project from graph elements or become evidence/proposals that must be accepted through validated application services before they change the graph.

## Consequences

Positive:

- Native Event Storming becomes a first-class capture experience.
- Sticky notes can carry typed metadata.
- The workbench can validate and review the board before model extraction.
- Every generated graph element can retain provenance back to a canvas sticky or imported workshop artefact.
- The same approach can later support collaboration, canvas persistence, and custom domain-specific shapes.

Negative:

- The UI now depends on a specialist canvas SDK.
- The first implementation must carefully isolate canvas-specific types from the canonical domain model.
- Full custom tools and multiplayer support are deferred until after the core workbench shell exists.

## Alternatives Considered

- **React Flow**: better suited to structured node/edge graph visualisation than freeform workshop capture. Still useful later for the architecture graph explorer.
- **Excalidraw**: excellent lightweight whiteboard, but less ideal for a typed, metadata-rich Event Storming model.
- **Miro embed/API**: valuable import/export option later, but not suitable as the primary embedded workbench canvas because it makes the core workflow dependent on an external SaaS board.
