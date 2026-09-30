# ADR-048: Keep Semantic Inference Candidates Separate from Accepted Facts

## Status

Accepted

## Context

Release 0.4 introduces deterministic bounded-context and semantic relationship inference. Repository layout, package cohesion, vocabulary, ownership, data access, APIs, and events can support a boundary, but each signal is incomplete. Multiple valid DDD interpretations may fit the same implementation, and conflicting evidence is architecturally meaningful.

If inference creates graph elements directly, confidence becomes authority and probabilistic-looking language obscures an automatic mutation. Reusing proposed-change status for inference would also collapse discovery, review, decision, and mutation into one concept.

## Decision

Represent inferred semantic concepts as immutable `SemanticCandidate` records outside the Architecture Knowledge Graph. A candidate retains its rule-set version, deterministic key, member units, supporting evidence, counter-evidence, confidence semantics, limitations, unresolved conflicts, and lifecycle history.

Use distinct states:

```text
CANDIDATE → UNDER_REVIEW
          → APPROVED_FOR_PROPOSAL | REJECTED | REFINEMENT_REQUIRED | DEFERRED
REFINEMENT_REQUIRED → SUPERSEDED by a new candidate revision
```

Candidate approval means only that an architect considers the inference suitable for formulating intended architecture. It does not create an accepted semantic fact.

Materializing intended architecture requires one or more concrete element, relationship, or constraint proposals. The existing Review Board and `ProposedArchitectureChange` boundary govern those proposals. Only explicit proposal acceptance creates or changes graph state.

Confidence describes evidence support. Lifecycle describes review state. Acceptance describes explicit mutation authority. These dimensions must remain separate in domain, API, events, persistence, and UI.

## Relationship to existing concepts

- AIM `Evidence`, `Observation`, and `Finding` carry the reasoning chain; candidates reference them rather than duplicating evidence.
- Product Modules are explicit partitions and may seed or contradict candidates; they are not automatically bounded contexts.
- Product recommendations remain governed options under ADR-044.
- The API may adapt a candidate to the existing Review Board, following ADR-046, while retaining the Product semantic candidate as source record.
- `ProposedRelationshipAddition` is reused for approved relationship candidates.

## Alternatives considered

### Create low-confidence graph elements

Rejected because confidence does not grant authority and graph consumers could treat the element as accepted architecture.

### Store candidates as proposed architecture changes immediately

Rejected because a candidate may need refinement, may describe several graph mutations, and has not yet become a recommendation or decision. It would overload proposal semantics and encourage automatic proposal generation from weak evidence.

### Store candidates as findings only

Rejected because candidates have membership, topology, refinement, recurrence, and supersession semantics beyond an assessed issue or strength. They still trace through AIM findings where material.

### Choose the highest-confidence candidate and discard alternatives

Rejected because alternative boundaries and disagreements are useful. The Workbench retains candidates and counter-evidence rather than averaging or ranking them into apparent truth.

## Consequences

- Inference can evolve without contaminating canonical intent.
- Architects can refine and compare candidate boundaries with complete evidence.
- Candidate, recommendation, Review Board, proposal, and accepted graph lifecycles remain explicit.
- More records and UI states are required, but each has one authority and meaning.
- Unknown regions remain visible instead of becoming low-confidence graph nodes.
- No semantic candidate can mutate architecture automatically.
