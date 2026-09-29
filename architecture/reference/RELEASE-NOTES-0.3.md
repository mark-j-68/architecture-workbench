# Architecture Workbench 0.3 — Product Architecture Intelligence

Release 0.3 makes Product the primary architecture subject above individual repositories and completes the North Star workflow from discovery to a governed graph change.

## Highlights

- Compose multiple repositories into retained Product workspaces while preserving repository and discovery-run identity.
- Combine deterministic discovery evidence across repositories with provenance, confidence, findings, and coverage.
- Analyze cross-repository dependencies, contracts, release and deployment coupling, ownership, modularity, and distributed-monolith risk.
- Generate evidence-backed recommendation candidates with alternatives, trade-offs, counter-evidence, confidence, and retained lifecycle history.
- Generate deterministic, historical Product Architecture Scorecard snapshots. Unsupported dimensions remain unscored, and insufficient evidence suppresses the aggregate.
- Submit recommendation candidates to the existing Review Board, record votes, retain decisions, and distinguish Candidate, Submitted, Under Review, Approved, Rejected, Deferred, and Proposed Change states.
- Convert an approved recommendation into an actual `ProposedArchitectureChange` only after a user supplies a concrete graph element.
- Require separate, explicit human acceptance before mutating the canonical Architecture Knowledge Graph.
- Retain append-only scorecard snapshots, Review Board snapshots, recommendation lifecycle events, typed audit events, and traceability through findings, evidence, analyses, Products, repositories, discovery runs, workspaces, and proposed changes.

## Current limitations

- Discovery is local and static; remote Git providers and runtime telemetry are not included.
- Product Modules and bounded-context candidates remain explicit rather than semantically inferred.
- Product recommendation proposals add graph elements; relationship proposals remain available through the lower-level workflow.
- Scorecard dimensions without deterministic support remain unscored.
- Reviewers are human or stub participants; there are no live model providers.
- Persistence remains file based or in memory. PostgreSQL, vector memory, and event sourcing are not included.
- Release 0.3 does not perform autonomous remediation or automatic architecture mutation.

See the [Release 0.3 reference](RELEASE-0.3-MULTI-REPO-PRODUCT-ARCHITECTURE.md) and [North Star demo](NORTH-STAR-DEMO-0.3.md) for the full workflow and boundaries.
