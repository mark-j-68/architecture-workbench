# ADR-044: Recommendations Are Governed Options, Not Automated Architecture Changes

## Status

Accepted

## Context

Product findings can support several valid responses. Treating a finding as a single remediation hides uncertainty, delivery cost, counter-evidence, and the possibility that current coupling is intentional.

## Decision

Recommendations are retained, traceable candidates derived only from findings. Significant candidates present alternatives and qualitative impact, effort, risk, prerequisites, trade-offs, outcomes, and success measures. Counter-evidence affects urgency and confidence. Further discovery and no change required are valid outcomes.

Recommendation generation, Review Board submission, status changes, and recommendation approval do not mutate Product composition or canonical graph state. Creating a Proposed Change is a separate explicit action, and canonical mutation still requires explicit acceptance through the established governance boundary.

## Consequences

Users can compare options and inspect their evidence before deciding. Recommendation history remains stable across repeated analyses through deterministic identity rules. The Workbench may appear less prescriptive, but its advice is more transparent and safer. Product Architecture Score, autonomous remediation, and semantic bounded-context inference remain out of scope.
