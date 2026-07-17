# Product Architecture Recommendations

## Purpose

Release 0.3.4 converts a retained Product architecture analysis into recommendation candidates. A candidate is a governed option, not an instruction and not a canonical graph mutation.

The lifecycle boundary is: finding → recommendation candidate → Review Board → explicit decision → proposed change → explicit acceptance → canonical mutation. This milestone implements the first transition and explicit hand-off operations only.

## Model and traceability

Each recommendation records its Product, analysis and composition version; concerns; supporting finding, indicator, and evidence identifiers; repositories and Product Modules; counter-evidence; rationale; confidence; priority; time horizon; impact and effort bands; risks; prerequisites; outcomes; measures; limitations; and history links. Estimates are qualitative because source discovery cannot precisely predict delivery cost.

Categories cover contract versioning and ownership, release and deployment decoupling, dependency cycles, shared domain/data, synchronous and messaging coupling, central-router governance, module boundaries, packaging, compatibility matrices, observability, further discovery, and no change required.

## Options, priority, and time horizon

Significant findings carry alternatives. A dependency cycle can be addressed through a stable contract, an asynchronous boundary, consolidation of artificially separated responsibilities, or temporary documented acceptance. Each option exposes benefit, cost, risk, prerequisites, applicability, and confidence.

Priority combines severity with confidence, affected scope, counter-evidence, and likely impact. It is not copied from finding severity. Time horizons are immediate, next release, near term, medium term, strategic, or monitor.

## Central routing and Product packaging

Central routing concentration alone leads to governance or monitoring options: keep the runtime thin, preserve domain contract ownership, externalise and version policy, retain audit/replay, and measure coordination. Separation options appear as alternatives when stronger coordination and ownership indicators exist; removal is never automatic.

Packaging options make mandatory and optional module dependencies explicit through composition manifests, supported configurations, compatibility declarations, and composition-aware routing policies.

## Uncertainty and positive outcomes

Counter-evidence reduces confidence and urgency. Incomplete evidence yields a further-discovery candidate instead of speculative restructuring. Strength findings yield no-change candidates that preserve, document, and monitor a healthy pattern.

## History, review, and persistence

Deterministic keys link recurring candidates across analysis runs. Exact scope matches increment recurrence and link the previous recommendation. Generations and lifecycle references are stored under the Product directory and covered by workspace integrity manifests.

Submission creates an explicit Review Board reference and changes status to `UNDER_REVIEW`. Creating a proposed change is also explicit and records intent. Neither operation mutates Product composition or the canonical graph; existing acceptance governance remains mandatory.

## Limitations

The engine does not estimate schedules or costs precisely, infer semantic bounded contexts, contact external Git providers, call live AI providers, calculate a Product Architecture Score, or autonomously apply remediation.
