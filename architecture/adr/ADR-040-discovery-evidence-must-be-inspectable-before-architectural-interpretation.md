# ADR-040: Discovery Evidence Must Be Inspectable Before Architectural Interpretation

## Status

Accepted

## Context

Release 0.2 can collect repository, language, framework, contract, messaging, and structural evidence. Release 0.3 will use that evidence for higher-level architectural interpretation. Users cannot reasonably trust later conclusions if the underlying evidence, confidence, provenance, derivation, and partial failures are hidden.

## Decision

Discovery runs have a persisted, immutable API read model and workspace-level evidence explorer. Users can inspect every plugin execution, evidence item, deterministic or narrowly heuristic observation, structural metric, warning, and isolated failure.

Confidence and provenance are first-class API and UI concepts. Derived observations and metrics retain supporting evidence identifiers. Plugin partial success remains useful and navigable. Deterministic observations use neutral wording and are never presented as smells or violations.

Release 0.2 does not present Product-level findings, architecture scores, risks, recommendations, or proposed changes through the evidence explorer. The legacy endpoint remains compatible but is a separate workflow.

Discovery-run persistence uses local workspace files and participates in workspace checksum integrity. The implementation remains synchronous and emits typed run-level start and completion events rather than event-sourcing individual evidence items.

## Consequences

- Users can verify source locations and derivations before accepting later interpretation.
- Plugin failures and incomplete evidence are visible instead of silently discarded.
- Release 0.3 has a stable, inspectable trust foundation.
- The read model duplicates selected domain data intentionally to keep API contracts immutable.
- Large local runs may require pagination in a later milestone.

## Alternatives considered

### Show only findings and scores

Rejected because conclusions without inspectable support undermine trust and conflate Release 0.2 with Release 0.3.

### Expose discovery domain objects directly

Rejected because mutable/internal domain evolution would leak into the public API and UI.

### Store results only in process memory

Rejected because users must be able to reload and inspect previous runs.
