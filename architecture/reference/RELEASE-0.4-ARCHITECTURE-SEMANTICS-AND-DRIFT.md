# Release 0.4 — Architecture Semantics and Drift Intelligence

## Release objective

Release 0.4 answers:

> What architecture actually exists, how confidently can its semantic boundaries be inferred, and where has observed implementation diverged from governed intent?

Release 0.3 established Product-level structural intelligence. Release 0.4 adds deterministic semantic candidates, immutable architecture snapshots, intended-versus-observed comparison, and evidence-backed drift findings. It strengthens trustworthy architecture understanding before live model-based review.

Detailed models:

- [Bounded-context inference](BOUNDED-CONTEXT-INFERENCE.md)
- [Architecture snapshot model](ARCHITECTURE-SNAPSHOT-MODEL.md)
- [Architecture drift model](ARCHITECTURE-DRIFT-MODEL.md)
- [ADR-047: Separate Observed and Intended Architecture](../adr/ADR-047-separate-observed-and-intended-architecture.md)
- [ADR-048: Keep Semantic Inference Candidates Separate from Accepted Facts](../adr/ADR-048-keep-semantic-inference-candidates-separate-from-accepted-facts.md)

## Problem statement

Structural discovery can show repositories, packages, contracts, calls, events, schemas, cycles, ownership metadata, and release coupling. These facts do not by themselves establish a bounded context, intended dependency direction, or architecture violation. The current canonical graph also does not explicitly distinguish governed intent from discovered implementation.

Without that distinction, the Workbench could silently turn an inference into truth, treat every implementation change as drift, or let newly discovered code redefine architectural intent. Release 0.4 introduces explicit epistemic and temporal boundaries while preserving the Release 0.3 governance chain.

## User value

An architect can:

- inspect semantic boundary candidates and the evidence for and against them;
- refine, reject, or approve a candidate without mutating the graph;
- establish intended elements, relationships, and constraints through existing proposed-change governance;
- capture reproducible Product architecture snapshots;
- compare observed snapshots over time and compare observed architecture with intended architecture;
- distinguish ordinary change, drift, constraint violation, and incomplete evidence;
- trace a drift finding through snapshots, deltas, observations, source evidence, recommendations, Review Board decisions, and any later proposed graph change.

## Constitutional invariants

1. Evidence precedes observation and inference.
2. Inference candidates are not canonical facts.
3. Recommendations are options, not decisions.
4. Decisions do not mutate architecture.
5. Canonical graph mutation requires an explicit accepted `ProposedArchitectureChange`.
6. Deterministic rules constrain all Release 0.4 inference.
7. Conflicting evidence and reviewer disagreement remain visible.
8. Insufficient evidence produces `UNKNOWN` or an evidence gap, never a guessed boundary or drift finding.
9. Every material semantic assertion and drift conclusion retains evidence and derivation references.
10. Snapshot and outcome records remain available to later reasoning without introducing event sourcing.

## Scope

### Semantic boundary candidates

- deterministic bounded-context candidate inference over retained Product evidence;
- explicit supporting and counter-evidence, confidence, limitations, and unresolved regions;
- candidate review, refinement, rejection, and approval;
- governed conversion of an approved candidate into element and relationship proposals.

### Controlled semantic relationships

Retain the existing graph vocabulary where it already expresses the concept:

| Required meaning | Release 0.4 graph vocabulary | Reason |
|---|---|---|
| dependency | `DEPENDS_ON` | already canonical |
| context containment | `CONTAINS` | avoids inverse `BELONGS_TO_CONTEXT` duplication |
| capability implementation | `REALIZED_BY` from capability to implementer | avoids inverse `IMPLEMENTS_CAPABILITY` duplication |
| aggregate emits domain event | `EMITS` | existing DDD meaning |

Add only meanings that cannot be expressed without losing material semantics:

- `OWNS_DATA`
- `EXPOSES_API`
- `CALLS_API`
- `PUBLISHES_EVENT` for a context or deployable publishing an event contract
- `CONSUMES_EVENT`
- `SHARES_DATA_WITH`

`PUBLISHES_EVENT` is distinct from existing aggregate-to-domain-event `EMITS`. Relationship endpoint rules and direction are defined centrally. Labels and attributes may refine a relationship but must not create hidden relationship types.

Relationship state is represented by authority and storage boundary rather than by overloading `RelationshipType`:

| State | Representation | Canonical graph? |
|---|---|---|
| observed | evidence-backed assertion in `ObservedArchitectureProjection` | no |
| inferred | `SemanticCandidate` with support, counter-evidence, and confidence | no |
| intended | relationship in a frozen/current intended projection, sourced from accepted graph state | yes |
| proposed | `ProposedArchitectureChange` containing a relationship mutation | no mutation yet |
| accepted | canonical `Relationship` after explicit proposal acceptance; therefore part of intended architecture | yes |

Reuse existing `ProposedRelationshipAddition`. Add a relationship-removal mutation referencing the exact relationship ID and expected content hash so stale proposals fail safely. A semantic replacement is one governed proposal containing the expected removal and addition as one atomic relationship change; it must not leave an invalid intermediate graph. Release 0.4 does not infer or apply removals automatically.

### Observed and intended architecture

- **Observed Architecture Projection**: read-only assertions derived from discovery, composition, and deterministic analysis. It is versioned and evidence-backed, but does not mutate the canonical graph.
- **Intended Architecture Projection**: accepted canonical graph elements and relationships, accepted constraints, and ADR-linked intent within a Product scope.
- **Semantic Candidate Set**: inferred assertions awaiting review or governance. Candidates belong to neither projection as accepted fact.

The Architecture Knowledge Graph remains the governed source of intended architecture. Observed architecture is a derived Product projection. Matching an observed assertion to a graph element does not rewrite that element.

### Snapshots, deltas, and drift

- immutable Product `ArchitectureSnapshot` manifests;
- content-addressed frozen Product subgraph projections for intended state;
- observed assertion sets and evidence references;
- `ArchitectureDelta` comparison for observed-to-observed change and intended-to-observed conformance;
- evidence sufficiency gates and explicit `UNKNOWN` results;
- AIM findings, scorecard inputs, recommendation generation, and Review Board integration for supported drift.

## Non-goals

Release 0.4 does not add live model providers, LLM inference, vector databases, semantic memory, PostgreSQL, remote Git ingestion, CI/CD integration, autonomous remediation, event sourcing, multi-user authorization, ML training, or post-v0.4 Decision Intelligence expansion.

It also does not attempt a universal enterprise ontology, infer bounded contexts from names alone, make documentation authoritative without governance, or reconstruct arbitrary past state from audit events.

## Architecture changes

### New domain concepts

- `SemanticAssertion`: a typed subject-predicate-object statement with scope and provenance.
- `SemanticCandidate`: an inferred element or relationship assertion with rule version, evidence, counter-evidence, confidence, and lifecycle.
- `ArchitectureConstraint`: governed intent that can be evaluated deterministically, such as forbidden dependency or required ownership.
- `ObservedArchitectureProjection`: retained assertions from one analysis boundary.
- `IntendedArchitectureProjection`: retained references to accepted graph state and constraints.
- `ArchitectureSnapshot`: immutable manifest joining Product composition, repositories/revisions, graph projection, evidence, analysis, candidates, and optional scorecard.
- `ArchitectureDelta`: classified differences between two comparable projections.
- `DriftFinding`: an AIM-compatible finding whose observations reference delta entries and both sides of the comparison.

These are Product semantic-analysis concepts. They do not replace AIM `Evidence`, `Observation`, `Finding`, `Recommendation`, or `DecisionOutcome`, and they do not create a second canonical graph.

### Existing concepts reused

- Product, Product Module, composition version, repository membership, discovery runs, and evidence provenance;
- Architecture Knowledge Graph elements and controlled `RelationshipType`;
- `ProposedRelationshipAddition`, already supported by `ProposedArchitectureChange`;
- AIM evidence-to-decision chain;
- Product recommendation adapter and Review Board lifecycle;
- scorecard snapshot/history conventions;
- workspace-scoped file repository abstractions, atomic writes, integrity manifests, and typed audit events.

### Storage strategy

Store immutable manifests under the Product boundary. Large normalized graph and observed-assertion payloads are content-addressed by SHA-256 and reused across snapshots. A snapshot stores hashes and stable record identifiers, not copies of every discovery artifact. Retention is append-only at the domain level; a latest pointer is only a projection.

This is snapshot persistence, not event sourcing: state is read from current records and immutable snapshots, never rebuilt by replaying audit events.

### API boundary proposed for implementation

Workspace and Product scoped resources should expose:

- semantic inference runs and candidate sets;
- candidate detail, evidence, counter-evidence, and lifecycle commands;
- intended and observed projections;
- snapshot creation, history, and retrieval;
- delta creation and retrieval;
- drift findings and evidence traversal;
- recommendation submission through the existing governed lifecycle;
- relationship-level proposed changes through the existing proposal service.

Commands must use explicit verbs for candidate review and proposal creation. A generic update endpoint must not collapse epistemic states.

### Typed events proposed

Extend the existing typed audit vocabulary at material lifecycle boundaries:

- `PRODUCT_SEMANTIC_INFERENCE_STARTED`
- `PRODUCT_SEMANTIC_INFERENCE_COMPLETED`
- `BOUNDED_CONTEXT_CANDIDATE_RECORDED`
- `SEMANTIC_CANDIDATE_STATUS_CHANGED`
- `OBSERVED_ARCHITECTURE_PROJECTED`
- `INTENDED_ARCHITECTURE_PROJECTED`
- `ARCHITECTURE_SNAPSHOT_CREATED`
- `ARCHITECTURE_DELTA_CREATED`
- `ARCHITECTURE_CONSTRAINT_EVALUATED`
- `ARCHITECTURE_DRIFT_FINDING_CREATED`

Events carry stable record IDs, workspace/Product scope, causation/correlation, rule or schema version, and non-sensitive summary metadata. Evidence remains referenced. Events are audit and integration records, not a replay source.

## Milestone decomposition

### v0.4.1 — Semantic foundations and controlled relationship vocabulary

- define assertion provenance and epistemic states;
- add only the six justified relationship types and endpoint validation;
- define Product scoping between Product records and graph elements;
- expose governed relationship addition, removal, and atomic replacement by extending the existing sealed mutation boundary;
- add typed semantic-candidate and proposal audit events.

### v0.4.2 — Deterministic bounded-context candidates

- build evidence feature projections;
- implement versioned deterministic rules and unresolved-region reporting;
- retain candidate sets, support/counter-evidence, and confidence;
- provide review, refinement, rejection, supersession, and approval without graph mutation.

### v0.4.3 — Governed intended architecture

- derive intended Product projections from accepted graph state, constraints, and ADR links;
- convert approved semantic candidates into explicit element/relationship proposals;
- require separate proposal acceptance before canonical mutation;
- preserve candidate-to-decision-to-proposal traceability.

### v0.4.4 — Immutable architecture snapshots

- create snapshot manifests and content-addressed Product subgraph artifacts;
- capture repository revision availability and evidence coverage;
- link analyses, candidate sets, and scorecards;
- support snapshot history and reproducible retrieval.

### v0.4.5 — Architecture delta and drift intelligence

- compare observed snapshots and intended-versus-observed state;
- classify change, drift, violation, and evidence gap;
- create evidence-backed AIM observations and findings;
- retain rule versions, before/after references, severity, confidence, and counter-evidence.

### v0.4.6 — Scorecard, governance, UI, and North Star integration

- feed supported semantic cohesion, directionality, and drift findings into existing scorecard dimensions;
- generate governed recommendations from drift findings;
- complete the minimum Product UI flow and evidence navigation;
- demonstrate the full Release 0.4 North Star scenario.

## Minimum UI workflow

```text
Product
→ compose evidence and run analysis
→ inspect observed semantic assertions
→ inspect bounded-context candidates and unresolved regions
→ review/refine/reject/approve a candidate
→ create and govern intended architecture proposals
→ create a snapshot
→ compare snapshots or intended versus observed
→ inspect classified delta and source evidence
→ generate a recommendation
→ submit to Review Board
→ create a proposed relationship or element change
→ explicitly accept before graph mutation
```

The UI must display distinct badges and explanations for `OBSERVED`, `INFERRED CANDIDATE`, `ACCEPTED INTENT`, `CHANGE`, `DRIFT`, `VIOLATION`, `UNKNOWN`, `RECOMMENDATION`, and `PROPOSED CHANGE`. Confidence is shown only for observations, candidates, and findings; acceptance state is never rendered as confidence.

## Scorecard extension

- Bounded-context cohesion becomes scorable only from reviewed candidate topology or accepted context definitions combined with deterministic cohesion/coupling evidence.
- Architecture drift becomes scorable only from comparable snapshots and supported drift/violation findings.
- Dependency directionality contributes through explicit intended constraints and observed dependency assertions.
- Product modularity may add context crossing and alignment findings while retaining existing structural indicators.

The existing weights, partial-score rule, null dimensions, evidence coverage, history, and read-only behavior remain. Release 0.4 adds mapped findings; it does not invent a new aggregate.

## Success criteria

- Every semantic candidate cites rule version, supporting evidence, counter-evidence, and confidence semantics.
- No candidate, observed assertion, snapshot, delta, finding, scorecard, or recommendation mutates the graph.
- Accepted graph mutations still require an explicit accepted `ProposedArchitectureChange`.
- Relationship vocabulary has documented direction and endpoint validation with no synonymous pairs.
- Snapshot retrieval is reproducible after later Product and graph changes.
- Delta classification never reports drift when intent is absent or evidence sufficiency fails.
- Drift findings traverse to before/after snapshots and original discovery evidence.
- Existing Release 0.3 APIs and historical records remain readable.
- Maven tests, frontend build, and a full North Star lifecycle remain green at release completion.

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| Naming similarity creates false contexts | naming is supporting evidence only; require independent signal families |
| Missing evidence looks like compliance | explicit observability requirements and `UNKNOWN` delta outcomes |
| Ontology grows without control | small enum extension, endpoint matrix, ADR review for later additions |
| Intended graph becomes a copy of observed code | observed state remains a projection; only governed proposals mutate intent |
| Snapshots duplicate large workspaces | content-addressed normalized artifacts and referenced evidence |
| Drift noise overwhelms users | intent-aware classification, materiality thresholds, suppression rationale, grouping |
| Candidate acceptance bypasses governance | approval authorizes proposal creation only; proposal acceptance remains separate |
| Scorecard overstates semantics | unsupported dimensions remain null and measured weight remains visible |

## North Star scenario

An architect opens the Release 0.3 multi-repository mortgage Product. Deterministic analysis identifies two strong bounded-context candidates and one unresolved shared-model region. The UI shows package cohesion, API/event ownership, schema ownership, vocabulary, cross-boundary calls, and counter-evidence.

The architect refines one candidate, rejects the shared-model candidate, and approves the refined candidate for proposal. A concrete bounded-context element plus `CONTAINS`, `OWNS_DATA`, `EXPOSES_API`, and `PUBLISHES_EVENT` relationships enter the existing Review Board and proposed-change workflow. Only explicit proposal acceptance updates intended architecture.

The architect creates a baseline snapshot. A later observed snapshot contains a new direct database access from another context and a synchronous API call that bypasses the intended event relationship. The delta classifies the new call as architectural change, the owned-data access as drift and a constraint violation, and one absent deployment relationship as unknown because deployment evidence is incomplete. The drift finding links both snapshots and source evidence, affects only supported scorecard dimensions, and generates a governed recommendation. No remediation is applied automatically.

## Dependencies on Release 0.3

Release 0.4 depends on retained Product composition and analyses, deterministic findings, recommendation metadata, scorecard history, Review Board sessions, actual proposed changes, explicit acceptance, graph persistence, typed events, and workspace integrity. It extends these boundaries and does not replace them.

## Open decisions before implementation

- Choose the canonical Product-scoping mechanism for existing graph elements: an explicit `productId` field or a governed Product root/containment path. Name matching is prohibited.
- Decide whether `ArchitectureConstraint` is a specialized graph element or a governed companion repository referenced by a Policy element. Either choice must use proposed-change acceptance and snapshot cleanly.
- Confirm which local discovery plugins can supply stable Git revision and change-coupling evidence without adding remote Git ingestion.
- Define the smallest atomic mutation shape for relationship replacement while keeping existing single-mutation proposals readable.

## Recommended deferrals within the theme

- Defer change-coupling inference when stable local commit evidence is unavailable; never approximate it with timestamps.
- Defer generalized natural-language domain modeling. Release 0.4 vocabulary evidence is deterministic token concentration with explicit exclusions.
- Defer automatic constraint extraction from ADR text; ADRs support intent only through governed structured links.
- Defer full graph diff/merge and arbitrary ontology editing. Release 0.4 compares Product-scoped semantic projections and uses a controlled vocabulary.
- Defer snapshot artifact garbage collection until retention references and recovery behavior are proven; unused artifacts are safer than broken history in the local file adapter.

## Exit criteria

Release 0.4 is complete when:

1. deterministic bounded-context candidates and unresolved regions are retained and reviewable;
2. observed, inferred, intended, proposed, and accepted states cannot be confused in API or UI models;
3. controlled semantic relationships and relationship proposals are supported;
4. immutable Product snapshots can be reproduced and compared;
5. deltas distinguish change, drift, violation, and insufficient evidence;
6. supported drift enters AIM, scorecards, recommendations, Review Board, and proposed changes with full traceability;
7. the North Star scenario passes end to end;
8. no out-of-scope provider, persistence, ingestion, remediation, authentication, or event-sourcing capability has been introduced.
