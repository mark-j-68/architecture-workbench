# Architecture Snapshot Model

## Purpose

An `ArchitectureSnapshot` is an immutable, reproducible manifest of Product architecture knowledge at a point in time. It supports comparison after Product composition, discovery evidence, semantic analysis, or intended graph state changes. It does not replace the current graph, Product stores, audit log, or analysis history.

The separation of snapshot projections follows [ADR-047](../adr/ADR-047-separate-observed-and-intended-architecture.md).

## Snapshot identity

A snapshot has:

- `snapshotId`: opaque stable identity;
- `workspaceId` and `productId`;
- `snapshotKind`: `OBSERVED`, `INTENDED`, or `COMBINED`;
- `createdAt`, actor, causation ID, and correlation ID;
- optional human label and rationale;
- `schemaVersion` and producer/rule-set versions;
- content hash over the canonical manifest;
- optional `previousSnapshotId` as navigation, not reconstruction.

Snapshot IDs are unique even when content hashes match. A repeated capture can therefore record that the same architecture was observed twice without duplicating content artifacts.

## Snapshot manifest

### Product and composition

- Product identity and composition version;
- Product Module IDs and membership-state hash;
- repository membership IDs and roles;
- repository revision descriptors;
- evidence-coverage and completeness diagnostics.

### Repository revisions

Each repository descriptor records source identity, source reference, discovery-run IDs, and a revision value only when deterministically available, such as a Git commit SHA supplied by local discovery. If revision is unavailable, the manifest records `UNKNOWN` and relies on discovery-run and evidence hashes. A filesystem modification time is not a revision.

### Observed architecture

- observed-projection ID and content hash;
- semantic assertion set artifact hash;
- inference-run and candidate-set IDs;
- analysis ID and dependency-composition version;
- evidence-set hash plus stable evidence IDs;
- unresolved regions and evidence diagnostics.

### Intended architecture

- graph ID;
- frozen normalized Product subgraph artifact hash;
- accepted constraint-set ID/hash;
- accepted bounded-context and semantic relationship IDs;
- ADR references used as governed intent;
- graph schema and relationship-taxonomy versions.

An ADR reference contributes intent only when a governed graph element, relationship, or constraint explicitly links to it. Merely finding an ADR file does not make its text authoritative.

### Linked projections

- optional scorecard ID;
- Product analysis ID;
- recommendation-generation IDs where relevant;
- Review Board and proposed-change IDs that explain intended state;
- prior comparable snapshot IDs.

Links describe what was available at capture time. Later review or scorecard generation does not modify the snapshot.

## Provenance

Every manifest reference includes record type, stable ID, content hash where available, producer, producer version, and capture time. Evidence remains in the existing discovery/Product stores and is referenced rather than copied.

The snapshot creation command validates that all referenced records belong to the same workspace and Product, that referenced composition and analysis versions are compatible, and that content hashes can be read before the snapshot is committed.

## Lifecycle

```text
REQUESTED → MATERIALIZING → COMPLETE
                        ↘ FAILED
COMPLETE → RETAINED → ARCHIVED
```

`COMPLETE` snapshots are immutable. Failure records retain diagnostics but are not comparable snapshots. `ARCHIVED` changes retention visibility, not content. Deletion, if later required by workspace policy, must be explicit and cannot silently break retained delta references.

## Storage abstraction

Introduce a workspace-scoped `ArchitectureSnapshotRepository` with operations to append a manifest, resolve by ID, list Product history, and resolve content artifacts. File and in-memory adapters follow existing repository boundaries.

A file layout may use:

```text
products/{productId}/architecture-snapshots/{snapshotId}.json
products/{productId}/architecture-artifacts/{sha256}.json
products/{productId}/architecture-snapshot-latest.json
```

The latest file is a replaceable projection. Snapshot manifests and content-addressed artifacts are logically append-only and participate in the workspace integrity manifest.

## Graph/reference strategy

A pointer to the mutable current graph is insufficient because later accepted changes would alter the meaning of an older snapshot. Release 0.4 therefore freezes the Product-relevant intended graph projection at snapshot time.

The normalized artifact contains only Product-scoped element and relationship representations needed for comparison:

- stable IDs, types, names, descriptions, governed semantic metadata, and relevant attributes;
- stable relationship IDs, endpoint IDs, types, labels, and relevant attributes;
- accepted constraint references and ADR links.

The artifact is sorted canonically and addressed by SHA-256. Identical graph projections share one artifact. Discovery evidence, audit envelopes, Review Board sessions, and source files are not embedded.

Product scoping must be explicit. Release 0.4 cannot infer Product membership merely from graph names; accepted graph elements need a stable Product reference or a governed containment path.

## Observed assertion artifact

Observed architecture uses the same normalized assertion representation for comparison but remains outside the canonical graph. Each assertion contains:

- stable deterministic key;
- subject and object source identities;
- controlled predicate;
- observation status and confidence;
- supporting/counter-evidence IDs;
- derivation/rule ID and version;
- observability requirements and completeness.

Observed assertion artifacts are content-addressed and reused like graph artifacts.

## Analysis and scorecard linkage

An observed or combined snapshot requires an analysis ID and the exact Product composition/dependency versions consumed. An intended-only snapshot may omit analysis.

A scorecard link is optional because scorecard creation is separately controlled and read-only. The snapshot stores the linked scorecard ID if one existed at capture time. A later scorecard may reference the snapshot, but the snapshot remains unchanged. Comparisons must not select a newer scorecard implicitly.

## Comparison semantics

A comparison is valid only when:

- workspace and Product identities match;
- assertion and relationship taxonomy versions are compatible or a declared mapper exists;
- relevant repository identities can be matched without ambiguity;
- each rule declares whether required evidence is observable in both sides.

Comparison modes:

1. `OBSERVED_TO_OBSERVED`: describes implementation change over time.
2. `INTENDED_TO_OBSERVED`: evaluates conformance and possible drift.
3. `INTENDED_TO_INTENDED`: describes governed intent evolution; it is not drift.
4. `COMBINED_TO_COMBINED`: produces separate observed-change, intended-change, and conformance sections rather than flattening them.

A snapshot difference is not itself a finding. `ArchitectureDelta` classifies comparable differences using the drift model.

## Retention and versioning

- Complete snapshots and referenced deltas are retained by default for the Product lifetime.
- Schema evolution uses explicit manifest versions and read adapters; existing manifests are not rewritten in place.
- Rule and taxonomy versions are part of comparability checks.
- Superseding a snapshot creates a new snapshot and link.
- Content artifacts may be garbage-collected only when no retained snapshot references them and workspace retention policy permits it.
- Existing Release 0.3 analysis, scorecard, review, and lifecycle histories remain independent records.

## Distinction from event sourcing

Snapshots capture selected state directly from current authoritative stores and retained projections. Audit events explain actions and provide traceability, but they are not replayed to reconstruct a snapshot or current state. The application continues to load current Product and graph state from repositories.

Architecture snapshots therefore provide temporal comparison without adopting event streams as the system of record, aggregate replay, event upcasting, or event-sourced write models.

## Failure and partial-evidence behavior

- A missing optional scorecard produces no failure.
- Missing required analysis, corrupt content, or mismatched Product scope fails snapshot creation.
- Repositories without revision evidence are retained as `UNKNOWN` with diagnostics.
- Partial discovery can produce a complete snapshot with explicit evidence coverage, but later delta rules must honor that limitation.
- Hash or workspace-integrity failure blocks snapshot completion.

## Minimal examples

### Observed snapshot

Snapshot `S1` references Product composition 7, two discovery runs with local commit SHAs, analysis `A7`, candidate set `C7`, observed assertion artifact `sha256:o1`, and scorecard `SC7`. It has no graph artifact because it is observed-only.

### Combined snapshot

Snapshot `S2` references observed artifact `sha256:o2` and intended Product subgraph `sha256:g4`. The current graph later changes to `g5`; `S2` still resolves `g4`, so its conformance result is reproducible.

### Repeated content

Snapshot `S3` is captured a week after `S2` with new discovery-run IDs but normalized observed content still hashes to `o2`. The artifact is reused; the distinct manifest preserves time, runs, revisions, and evidence coverage.
