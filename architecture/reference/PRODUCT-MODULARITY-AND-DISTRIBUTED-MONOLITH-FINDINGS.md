# Product Modularity and Distributed Monolith Findings

## Purpose and boundary

Release 0.3.3 is the first interpretation stage above neutral Product composition. It consumes retained repository evidence and the versioned Product dependency composition. It produces explainable Product observations, strengths, risk findings, and a distributed-monolith assessment. It does not calculate a Product Architecture Score, prescribe remediation, infer semantic bounded contexts, propose changes, or mutate the canonical graph.

## Analysis model

Each analysis identifies the exact dependency-composition version used and is retained independently. A finding has a polarity (`STRENGTH` or `RISK`), AIM-aligned concern, severity, confidence category and numeric value, indicators, evidence references, repositories and explicitly supplied Product Modules, exact dependency paths where relevant, derivation, counter-evidence, limitations, and timestamp. Findings adapt to the canonical AIM concepts at the API boundary rather than creating a competing evidence model.

Supported concerns are modularity, release and deployment independence, contract maturity, bounded ownership, communication complexity, data ownership, product packaging, operational coupling, governance, and evolvability.

## Indicator catalogue

The deterministic aggregation recognises explicit release and deployment lockstep, cross-repository cycles, shared domain artifacts, contract immaturity, shared database/schema identities, bidirectional synchronous dependencies, messaging concentration, central routers, supplied Product Module boundary mismatch, ownership coupling, and packaging coupling when corresponding evidence exists. Naming-only shared-domain evidence is deliberately lower confidence. Identical release versions alone do not establish release lockstep. Shared channels alone do not establish unhealthy messaging coupling.

Strength indicators include independent versioning, explicit contract ownership, high explicit-version coverage, an acyclic composed dependency graph, and independently organised modules where supported.

## Aggregation, severity, and confidence

High-impact classifications require multiple independent indicators. `DISTRIBUTED_MONOLITH_CONFIRMED` is reserved for strong, explicit lockstep release, lockstep deployment, and pervasive cyclic coupling. Other classifications are `INSUFFICIENT_EVIDENCE`, `MODULAR`, `MOSTLY_MODULAR`, `COUPLED`, and `DISTRIBUTED_MONOLITH_RISK`.

Severity expresses likely Product impact (`INFO`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`). Confidence is `CONFIRMED`, `HIGH`, `MEDIUM`, `LOW`, or `INSUFFICIENT_EVIDENCE`; it incorporates directness, independent evidence, coverage, partial discovery, ambiguity, and counter-evidence. An explanation and derivation are mandatory.

## Counter-evidence and central routing nuance

Counter-evidence is first-class and reduces confidence. A concentrated router may be a healthy thin runtime when policy and contracts remain externally owned, versioned, independently deployable, auditable, and free of direct service calls. The analysis therefore distinguishes presence and coordination load and states explicitly that concentration alone is not proof of ESB drift.

## Lifecycle, API, persistence, and traceability

Analysis starts only after Product evidence and Product dependencies have been composed. The API supports creating and retrieving analyses, filtered findings, finding detail, the latest distributed-monolith assessment, and analysis history. Files are retained under `products/{productId}/architecture-analyses/`, with `architecture-analysis.json` as the latest view; all participate in workspace integrity manifests.

Every indicator carries dependency and original evidence identifiers. Dependency references preserve repository, discovery run, plugin, file, and symbol provenance. Missing discovery runs and unresolved dependency diagnostics are surfaced and reduce evidence coverage.

## Limitations

This release uses static, discovered evidence. Lack of an indicator means “not detected,” not proof of absence. No runtime traces, external Git-hosting APIs, semantic bounded-context inference, architecture score, recommendation, Review Board decision, or graph mutation is introduced.
