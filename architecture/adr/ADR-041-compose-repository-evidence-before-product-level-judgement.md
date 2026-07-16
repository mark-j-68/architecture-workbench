# ADR-041 - Compose Repository Evidence Before Product-Level Judgement

## Status

Accepted

## Context

Release 0.2 produces evidence, observations, and metrics within repository discovery runs. Products commonly span multiple repositories, but immediately interpreting aggregated data would hide source authority and make ambiguous technical identities look certain.

## Decision

Product is the primary analysis boundary above Repository and belongs to one Workspace. Repository discovery remains authoritative at source. Product composition stores membership and run references, then builds read-only projections that preserve repository, run, evidence, plugin, confidence, classification, and file/symbol provenance.

Exact technical identities may create deterministic cross-repository relationships. Ambiguous identities remain explicit conflicts and are not silently merged. Composition emits typed lifecycle events but does not mutate the canonical graph or introduce event sourcing.

## Consequences

Users can organize repositories into explicitly defined Product Modules, reload compositions from file storage, trace every relationship to evidence, and inspect resolution conflicts. Product files participate in workspace integrity manifests.

Product-level smells, distributed-monolith and ESB classification, bounded-context inference, architecture scoring, recommendations, and proposed changes are deferred to later Release 0.3 milestones.
