# ADR-046: Adapt Product Recommendations at the Governance Boundary

## Status

Accepted

## Context

Release 0.3 Product analysis retains a detailed `ProductArchitectureRecommendationView`. The established Review Board accepts the Architecture Intelligence Model `Recommendation`, while the Architecture Knowledge Graph accepts only a concrete `ProposedArchitectureChange` containing an element or relationship mutation. Treating these three records as interchangeable would either discard Product traceability or let a recommendation imply a graph mutation.

The current Review Board decision vocabulary also says `ACCEPT_PROPOSED_CHANGE`, including when a session reviews a recommendation before a graph proposal exists. Product workflow state must describe that outcome as approval of the option, not acceptance of a graph mutation.

## Decision

Keep the Product recommendation as the retained source record and adapt it to the Architecture Intelligence Model only at the Review Board API boundary. The adapter reconstructs AIM findings, observations, and evidence from the retained Product analysis while preserving their identifiers. Review Board sessions contain the adapted recommendation and Product review snapshots retain the Product, workspace, analysis, composition, repository, discovery-run, finding, evidence, session, and proposal references.

Translate a Review Board `ACCEPT_PROPOSED_CHANGE` result for a recommendation-only session to Product recommendation state `APPROVED`. This state authorizes the user to prepare a proposal; it does not create or accept one. The user must separately provide a concrete graph mutation. That command uses the existing `ProposedChangeService` and retains the Product recommendation ID plus finding and evidence IDs. A further explicit acceptance command is required before canonical graph mutation.

Retain Product review snapshots and recommendation lifecycle events as append-only history. The existing Review Board store may continue to hold the current session representation; Product history supplies the immutable workflow record required by Release 0.3.

Scorecards remain read-only projections and are not inputs to Review Board decision derivation.

## Consequences

The integration reuses the existing Architecture Intelligence Model, Review Board, and proposed-change concepts without duplicating them. Product state names distinguish option approval from graph-change acceptance. The adapter is intentionally local to `architecture-api`; core modules do not depend on Product API DTOs.

Release 0.3 supports concrete element-addition proposals from Product recommendations. Relationship proposals require the UI to select existing source and target graph elements and remain future work.
