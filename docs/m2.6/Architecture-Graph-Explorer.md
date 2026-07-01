# M2.6 — Architecture Graph Explorer

## Purpose

The Architecture Graph Explorer provides a structured, navigable view of the relationships inside an ArchitectureModel.

It is deliberately different from the Event Storm Canvas:

- Event Storm Canvas: freeform capture using tldraw.
- Architecture Graph Explorer: structured model exploration using React Flow.

## Flow

```text
ArchitectureModel
  ↓
ArchitectureGraphBuilder
  ↓
ArchitectureGraph
  ↓
React Flow graph explorer
```

## Why it matters

The graph becomes the visual reasoning surface for:

- service ownership
- aggregate boundaries
- command-to-aggregate routing
- event publication and consumption
- policy triggers
- read-model population
- deployment resource ownership
- AI judge and audit relationships
- future impact analysis

## Initial UI Capabilities

- Render typed nodes and labelled edges.
- Use minimap, zoom and pan controls.
- Filter nodes by type.
- Select a node and expose metadata to a side panel.
- Use dark-mode styling consistent with Architecture Workbench.

## Future Capabilities

- Highlight upstream/downstream dependencies.
- Show blast radius for a changed aggregate, event or service.
- Overlay validation findings on affected nodes.
- Overlay AI review findings on affected subgraphs.
- Compare graph changes between model versions.
- Export graph to Mermaid, GraphML or Neo4j.

## Implementation Notes

React Flow is imported from `@xyflow/react` and uses its default stylesheet:

```ts
import { ReactFlow, Background, Controls, MiniMap, Panel } from '@xyflow/react'
import '@xyflow/react/dist/style.css'
```

The backend projection should remain deterministic and AI-free. AI can explain or review graph relationships, but it should not invent them.
