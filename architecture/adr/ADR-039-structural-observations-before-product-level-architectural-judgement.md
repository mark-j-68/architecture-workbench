# ADR-039: Structural Observations Before Product-Level Architectural Judgement

## Status

Accepted

## Context

Release 0.2 discovery evidence now describes repositories, Maven and Java structure, Spring applications, contracts, and communication topology. That evidence can deterministically reveal dependency cycles, direction, connectivity counts, version coverage, and topology paths.

These shapes are useful facts, but their architectural meaning depends on Product boundaries, ownership, release independence, operational context, and deliberate design decisions. Treating a cycle or central routing node as a smell during discovery would collapse evidence collection and Product-level interpretation into one stage.

## Decision

Release 0.2 structural-analysis plugins may produce evidence-backed deterministic observations and narrowly scoped heuristic observations for:

- package and module cycles and dependency directions;
- fan-in, fan-out, connectivity, and dependency paths;
- candidate technical layers;
- explicit, absent, multiple, deprecated, matching, mismatching, or unresolved contract versions;
- producer, consumer, channel, route, hop, routing-node, and recovery-topology shape;
- traceable structural metrics and version coverage.

Every output must preserve confidence, observed/inferred classification, workspace context, supporting evidence identifiers, and an explanation of its derivation. Cycle and path output must preserve the exact edge sequence. Traversal must be bounded and expose truncation through diagnostics and partial-success status.

Release 0.2 analysis must not produce architectural findings, risk classification, recommendations, proposed changes, Product scorecards, or direct canonical graph mutations.

Smell classification, distributed-monolith risk, bounded-context interpretation, ESB drift, architecture scoring, and recommendations belong to Release 0.3 and the Architecture Intelligence Engine. If later interpretation suggests a canonical graph addition, it must use the Proposed Change workflow and explicit acceptance.

## Consequences

Positive consequences:

- structural facts are reproducible and explainable;
- later Product analysis can reuse the same observations without rescanning source;
- centrality, cycles, and mismatches remain available without prematurely judging intent;
- partial evidence and scale limits remain visible rather than silently changing conclusions.

Trade-offs:

- Release 0.2 users see factual structure without a health verdict;
- absence observations require careful wording because discovery evidence can be incomplete;
- Release 0.3 must combine structural observations with Product, ownership, release, and runtime context before interpreting risk.

## Alternatives considered

### Produce architecture findings directly from structural plugins

Rejected because identical dependency shapes can have different meaning in different Product and operational contexts.

### Defer all graph calculation to Release 0.3

Rejected because cycles, paths, counts, and literal version comparisons are deterministic reusable derivations that belong beside discovery evidence.

### Mutate the canonical graph from analysis output

Rejected because discovery and analysis output is not accepted architectural state and must not bypass Proposed Change governance.
