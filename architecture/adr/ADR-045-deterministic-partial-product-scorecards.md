# ADR-045: Deterministic Partial Product Scorecards

## Status

Accepted

## Context

The Product Architecture Scorecard design proposes twelve weighted dimensions and an overall score. Release 0.3.3 produces deterministic findings for some dimensions, but does not observe semantic bounded-context cohesion or architecture drift. Treating missing findings as a healthy score would conceal missing evidence.

## Decision

Generate immutable scorecard snapshots from a retained Product analysis. Each dimension has its design weight, a score only when a mapped deterministic finding exists, separate confidence, linked finding and evidence identifiers, and recommendation and review-status references from the same analysis. A dimension without supported indicators has a null score and zero confidence.

The dimension score starts at a neutral reference of 50. Each strength adds and each risk subtracts severity magnitude (INFO 5, LOW 10, MEDIUM 20, HIGH 30, CRITICAL 40) multiplied by finding confidence. The result is clamped to 0–100 and rounded. This is a transparent comparative index, not a probability, compliance grade, or independent assessment of absent evidence. The overall index is the design-weighted mean of scored dimensions only; it is withheld unless at least 50 percent of design weight is measured and Product evidence coverage is at least 50 percent. The snapshot exposes measured weight and confidence so a partial index cannot appear complete.

Metric confidence follows the design weights: evidence coverage 35 percent; mean finding confidence as evidence-quality proxy 25 percent; age of the retained analysis 15 percent; at least two distinct evidence identifiers as a limited corroboration proxy 15 percent; and a linked recommendation in ACCEPTED, IMPLEMENTED, or VERIFIED status as review-validation proxy 10 percent. Review submission alone does not validate a finding. These proxies are shown as limitations; they do not establish source recency or independent source agreement.

Snapshots retain the analysis ID, composition version, per-dimension links, generation time, prior snapshot ID, and score trend. Overall trends are reported only when both snapshots measure the same dimensions. Regenerating does not alter earlier snapshots, recommendations, Product composition, Review Board decisions, proposed changes, or the canonical graph.

## Consequences

Unobserved bounded-context cohesion and architecture drift remain unscored. Scores can change when the deterministic analysis changes or review metadata is updated; the snapshot preserves what was known at generation time. Additional discovery inputs may support more dimensions later without rewriting historical results.
