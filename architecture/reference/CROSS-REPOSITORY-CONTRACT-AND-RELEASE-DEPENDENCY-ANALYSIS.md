# Cross-Repository Contract and Release Dependency Analysis

## Purpose

Release 0.3.2 composes repository discovery evidence into a versioned, read-only Product dependency view. It describes technical relationships; it does not classify architecture risk.

## Dependency taxonomy

Dependencies are typed as API, event, command, message channel, Maven artifact/shared library, shared schema/database, release coordination, deployment order/environment configuration, or ownership coordination. Each edge names its source and target repository, direction, technical identity, literal versions, confidence, observed/inferred status, derivation, and evidence references.

An exact producer/consumer contract identity creates a directed contract edge. Exact queue, topic, or bus identities create shared-channel edges. Maven coordinates connect a module that publishes an artifact to another module that declares it. Exact deployment resource identities and explicit release metadata create neutral deployment and release relationships. Dynamic values remain unresolved.

## Version and compatibility semantics

`COMPATIBLE` means both ends declare the same literal version. `INCOMPATIBLE` is reserved for different literal versions on an exact producer/consumer identity. `UNKNOWN` means one or both literal versions are absent or dynamic. `NOT_APPLICABLE` is used for relationships without a compatibility claim. Unknown is never converted into incompatibility.

The repository compatibility matrix aggregates these relationships. An explicit contradiction takes precedence, followed by unknown, compatible, and not applicable.

## Release, deployment, and ownership evidence

Release relationships use explicit repository version metadata, release bundles/streams, and repository evidence from Maven, local release files, CI configuration, or deployment manifests. No remote Git-hosting API is called. Deployment composition recognizes exact shared infrastructure, environment, database, and channel identities; it does not infer criticality. Ownership combines membership metadata and explicit contract ownership evidence. Missing and conflicting owners are diagnostics, not judgements.

## Versioned composition and traceability

Every dependency composition receives a monotonically increasing Product composition version and records its timestamp, correlation id, input discovery-run ids, outputs, metrics, and diagnostics. Previous snapshots are retained in `dependency-compositions/`; `dependencies.json` points to the latest snapshot. Files participate in workspace integrity manifests.

Every relationship links to the repository, discovery run, Product evidence id, original evidence id, plugin where available, and file/symbol provenance. The API exposes dependencies, the graph, compatibility and matrix, release, deployment, ownership, metrics, and version history. Filters are intentionally limited to dependency type, repository, compatibility, and minimum confidence.

## Boundary

This milestone reports explicit facts and conservative identity correlations. It does not calculate distributed-monolith risk, ESB drift, bounded contexts, a Product Architecture Score, smells, recommendations, or proposed changes. Those require later Product-level interpretation.
