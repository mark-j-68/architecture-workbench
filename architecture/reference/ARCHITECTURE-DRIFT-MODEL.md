# Architecture Drift Model

## Purpose

Release 0.4 distinguishes implementation change from divergence against governed architectural intent. Drift is never inferred merely because two snapshots differ.

This model builds on the [Architecture Snapshot Model](ARCHITECTURE-SNAPSHOT-MODEL.md) and [ADR-047](../adr/ADR-047-separate-observed-and-intended-architecture.md).

## Core definitions

### Observed architecture

A read-only, evidence-backed projection of implementation and delivery characteristics derived from Product discovery, composition, dependency analysis, and semantic analysis. Observed assertions have confidence and observability requirements. They are not canonical graph facts.

### Intended architecture

Governed architectural intent represented by accepted Architecture Knowledge Graph elements and relationships, accepted constraints/policies, accepted bounded-context definitions, and explicit ADR links. Documentation discovered in a repository is evidence; it becomes intended authority only through a governed link or accepted change.

### Architecture delta

An immutable comparison result between two architecture snapshots or projections. A delta contains entries classified by comparison mode and rule. It describes differences and absence; it does not automatically claim harm or non-compliance.

### Architectural change

A supported difference between comparable observed snapshots or intended snapshots. Examples include a new dependency, removed API, ownership change, or governed boundary update. Change may be neutral, beneficial, harmful, or unknown.

### Architecture drift

A supported observed condition that contradicts governed intended architecture at the comparison point. Drift requires:

1. explicit applicable intent;
2. sufficient evidence that the condition is observable;
3. a deterministic contradiction rule;
4. traceability to both intent and observed evidence.

### Constraint violation

A failed deterministic `ArchitectureConstraint`. A violation may also be drift, but the concepts remain separate: drift compares observed and intended architecture; a violation evaluates a governed rule. One delta entry may be both `DRIFT` and `CONSTRAINT_VIOLATION` with separate rationale.

### Incomplete evidence

A result where a required condition cannot be observed reliably. It is classified `EVIDENCE_GAP` or `UNKNOWN`, never compliant, absent, or drift.

## Intended architecture sources

Intent is assembled from:

- accepted graph elements and relationships scoped to the Product;
- accepted bounded-context definitions and semantic relationships;
- accepted `ArchitectureConstraint` records;
- governed proposed changes after explicit acceptance;
- ADR graph elements explicitly linked to the intended element, relationship, or constraint;
- explicit policy graph elements where their machine-evaluable constraint is governed.

Recommendation approval, candidate approval, unaccepted proposals, repository documentation, inferred candidates, and observed projections are excluded from accepted intent.

## Architecture constraints

A minimal constraint model contains:

- constraint ID, Product/workspace scope, type, subject selector, object selector, and relationship predicate;
- expectation: `REQUIRED`, `FORBIDDEN`, `ALLOWED_ONLY_VIA`, `OWNED_BY`, or `DIRECTION`;
- applicability conditions;
- observability requirements;
- severity and rationale;
- supporting decision/ADR/proposed-change references;
- lifecycle and effective interval;
- rule version.

Constraints are canonical intent and therefore enter the graph or an explicitly governed constraint repository only through proposed-change acceptance. Free-form policy text may support review but cannot produce an automatic violation without a deterministic expression.

## ArchitectureDelta model

An `ArchitectureDelta` contains:

- delta ID, workspace/Product IDs, comparison mode, and timestamps;
- left and right snapshot IDs;
- taxonomy and comparison-rule versions;
- overall evidence sufficiency and diagnostics;
- a list of immutable `DeltaEntry` values;
- summary counts by classification and severity;
- generation actor, causation, and correlation IDs.

Each `DeltaEntry` contains:

- stable deterministic key;
- subject, predicate, and object identities;
- change operation: `ADDED`, `REMOVED`, `CHANGED`, `PRESENT`, or `ABSENT`;
- primary classification: `CHANGE`, `DRIFT`, `CONSTRAINT_VIOLATION`, or `EVIDENCE_GAP`;
- optional secondary classifications;
- materiality/severity and confidence, each with explanation;
- applicable intent and constraint IDs;
- left/right assertion IDs;
- supporting and counter-evidence IDs;
- observability result and missing evidence;
- rule ID/version and derivation.

Delta generation is read-only and append-only. Re-running comparison produces a new delta linked to the same snapshots and rule version.

## Comparison rules

### Observed to observed

This mode reports architecture change. It may label a new condition as risky through a deterministic finding rule, but it cannot call the condition drift unless the comparison also resolves applicable intent.

Examples:

- a new dependency is `CHANGE`;
- a reversed dependency is `CHANGE`;
- an owner change is `CHANGE`;
- a missing relationship is `EVIDENCE_GAP` when the later snapshot lacks required evidence.

### Intended to observed

This mode evaluates conformance:

- observed relation contradicts forbidden relation → `DRIFT` plus `CONSTRAINT_VIOLATION`;
- required intended relation is observably absent → `DRIFT`;
- accepted context ownership contradicted by direct external data access → `DRIFT`, and violation if constrained;
- intended item is not observable with available plugins → `EVIDENCE_GAP`;
- observed relationship with no relevant intent → `CHANGE` or `UNSPECIFIED`, not drift.

### Intended to intended

This mode reports governed architecture evolution. Accepted replacement of an event interaction with a synchronous API is an intended `CHANGE`, not drift. A later observed comparison determines conformance.

## Initial deterministic drift rules

| Rule | Intent required | Observed evidence required | Result |
|---|---|---|---|
| `PROHIBITED_DEPENDENCY_PRESENT` | forbidden `DEPENDS_ON` constraint | deterministic dependency path | drift + violation |
| `DEPENDENCY_DIRECTION_REVERSED` | intended direction or direction constraint | comparable directed dependency | drift |
| `CONTEXT_BOUNDARY_CROSSED` | accepted context containment and crossing rule | implementation dependency across members | drift or change based on rule |
| `FOREIGN_DATA_ACCESS` | accepted `OWNS_DATA` plus exclusive-access constraint | direct external read/write evidence | drift; write may raise severity |
| `API_BYPASS` | required `EXPOSES_API`/`CALLS_API` path or allowed-only-via constraint | direct implementation/data dependency bypass | drift + optional violation |
| `EVENT_TO_SYNC_SUBSTITUTION` | intended publish/consume relationship | synchronous call replacing or bypassing it | drift when substitution is observable |
| `NEW_SHARED_DATA_COUPLING` | separate accepted context ownership | `SHARES_DATA_WITH` or multi-writer evidence | drift |
| `REQUIRED_BOUNDARY_ABSENT` | accepted bounded context plus observable membership expectation | sufficient semantic/member evidence | drift; otherwise evidence gap |
| `OWNERSHIP_CHANGED` | governed owner intent | explicit conflicting owner evidence | drift |
| `UNEXPECTED_RELATIONSHIP` | closed-world constraint for a controlled predicate | new observed relation | drift; without closed-world intent it is change |
| `INTENDED_RELATIONSHIP_NOT_OBSERVED` | required relation | plugin/evidence coverage able to observe it | drift; otherwise evidence gap |

Absence rules are valid only when the evidence producer declares that it can observe the relationship type and coverage is sufficient for both endpoints.

## Evidence sufficiency

Each drift rule declares required signal families, endpoint coverage, minimum source confidence, and whether absence can be concluded. Evaluation returns:

- `SUFFICIENT`: rule may classify drift or conformance;
- `PARTIAL`: report change/evidence gap with reduced confidence;
- `INSUFFICIENT`: report `UNKNOWN` and missing evidence;
- `CONFLICTING`: retain support and counter-evidence and avoid a definitive drift conclusion unless the rule explicitly resolves it.

Repository removal, plugin failure, unknown revision, ambiguous identity, stale intended scope, and taxonomy mismatch can reduce sufficiency.

## Severity and confidence

Severity represents architectural impact and comes from the rule plus contextual materiality:

- `CRITICAL`: governed critical constraint violation with broad or irreversible impact;
- `HIGH`: boundary/data ownership breach, prohibited cycle, or mandatory coordination across critical contexts;
- `MEDIUM`: material unexpected coupling or required relation absent with bounded impact;
- `LOW`: local divergence with limited scope;
- `INFO`: supported neutral or beneficial change.

Confidence represents support for the conclusion, not severity or acceptance. It combines the rule’s required evidence directness, identity certainty, endpoint coverage, snapshot comparability, and counter-evidence. The explanation lists these components. Conflicting evidence is retained and caps confidence according to the rule.

## AIM integration

The existing chain remains:

```text
Evidence → Observation → Finding → Concern → Recommendation → DecisionOutcome
```

- Source discovery evidence remains AIM `Evidence` through the existing adapter boundary.
- A delta entry becomes an `Observation` only when its comparison and evidence-sufficiency rules complete.
- A material supported drift, violation, or evidence gap becomes a `Finding` with category, severity, confidence, and delta references.
- Concerns reuse modularity, bounded ownership, data ownership, communication complexity, governance, evolvability, and architecture drift.
- Recommendations remain alternatives with counter-evidence and may validly recommend further discovery, accept/document current divergence, update intent, or change implementation.
- Review Board decisions and `DecisionOutcome` references never mutate graph state.

Drift findings retain left/right snapshot IDs, delta ID/entry key, intent IDs, source evidence IDs, and Product analysis context.

## Recommendation and governance integration

1. Generate recommendations only from retained drift findings.
2. Preserve alternatives: restore implementation, revise governed intent, add a constraint, accept/document divergence, or gather evidence.
3. Submit the chosen recommendation through the existing Product Review Board adapter.
4. On approval, require a concrete `ProposedArchitectureChange` for any graph element, relationship, or constraint update.
5. Require separate explicit proposal acceptance before mutation.
6. Implementation remediation remains external and manual in Release 0.4.

Choosing to update intended architecture must not retroactively erase the earlier drift. Later snapshots show the governed intent change and subsequent conformance.

## Scorecard integration

The `ARCHITECTURE_DRIFT` dimension receives only supported drift and constraint-violation findings from comparable snapshots. Evidence gaps do not reduce or improve the score; they keep the dimension unscored or reduce evidence coverage according to existing scorecard rules.

Bounded-context cohesion, dependency directionality, and Product modularity may consume their own deterministic semantic findings. The scorecard remains historical, read-only, partial, and independent of Review Board decisions.

## Examples

### New dependency without intent

Snapshot B adds `Pricing DEPENDS_ON Customer`. No governed constraint or expected direction applies. The delta reports `CHANGE`. A coupling finding may be created if deterministic structural rules support it, but it is not drift.

### Foreign data access

The intended graph states `Origination OWNS_DATA LoanApplicationSchema` and an accepted constraint forbids external writes. Observed snapshot B shows Pricing writing that schema. The entry is `DRIFT` and `CONSTRAINT_VIOLATION`, with high severity and links to the accepted relation, constraint, call site, schema evidence, and both snapshots.

### Missing event with inadequate evidence

Intent requires Pricing to publish `QuoteCalculated`. Snapshot B has no messaging evidence because the messaging plugin failed. The result is `EVIDENCE_GAP`, not drift and not conformance.

### Governed intent change

The Review Board approves and an architect explicitly accepts a proposed change allowing a synchronous Pricing API. Intended snapshots I1 and I2 show an intended `CHANGE`. The old observed behavior remains drift relative to I1; a later comparison against I2 may conform. History is not rewritten.
