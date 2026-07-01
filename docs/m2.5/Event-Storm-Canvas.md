# M2.5 — Native Event Storm Canvas

## Intent

Architecture Workbench should support two capture modes:

1. Importing an existing JPEG/PNG of a workshop board.
2. Creating and maintaining a native Event Storming board inside the workbench.

The native canvas is preferred when the workshop is being run in the tool because every sticky can be captured as structured data rather than inferred from pixels.

## UI Decision

Use `tldraw` in `workbench-ui` for the embedded infinite canvas. The first implementation uses standard tldraw shapes plus metadata to identify Event Storming sticky types. Later versions may introduce full custom shapes and custom tools.

## Sticky Types

| Type | Colour convention | Model target |
|---|---|---|
| Domain Event | Orange | `DomainEvent` |
| Command | Yellow | `Command` |
| Aggregate | Blue | `Aggregate` |
| Policy | Purple | `Policy` |
| Read Model | Green | `ReadModel` |
| Hotspot | Red | validation finding / review item |
| External System | Grey | `ExternalSystem` |
| User Role | Pink | actor/persona |

## Model Mapping

`tldraw` shape data is normalised into `CaptureSection.eventStormBoards[]`.

```yaml
capture:
  event_storm_boards:
    - id: board-pos-001
      name: POS Event Storm
      source: tldraw
      stickies:
        - id: shape:abc
          type: DOMAIN_EVENT
          text: MortgageApplicationSubmitted
          position: { x: 1200, y: 420 }
          provenance: human-authored
          confidence: 1.0
```

The domain extractor then maps capture stickies into the canonical `domain` section, preserving source references so generated model elements can be traced back to canvas stickies.

## Why this matters

Image upload is useful for legacy boards, but native capture improves:

- traceability from generated architecture elements back to workshop notes;
- confidence scoring;
- validation while modelling;
- collaborative refinement;
- AI-assisted suggestions without losing human-authored provenance;
- regulatory auditability, because every canvas change can produce an activity-log envelope.

## M2.5 Scope

Included now:

- `workbench-ui` skeleton with `tldraw` dependency.
- `EventStormCanvas.tsx` component.
- capture model classes in `workbench-core`.
- event storm sticky validation rule.
- ADR-009.

Deferred:

- custom tldraw shape utilities.
- multiplayer collaboration.
- Miro import/export.
- image-to-canvas reconstruction.
- full activity logging of canvas deltas.
