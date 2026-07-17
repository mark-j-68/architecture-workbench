# ADR-043: Evidence Aggregation Before Distributed Monolith Classification

## Status

Accepted

## Context

Repository count and topology shape are weak proxies for Product modularity. A multi-repository system can remain release-, deployment-, data-, and ownership-coupled. Conversely, a central event router or shared infrastructure may be a deliberate thin runtime and is not automatically an ESB or architectural defect.

## Decision

Product-level findings aggregate multiple explicit indicators from a retained Product dependency composition. Every finding preserves its evidence chain, exact paths where applicable, severity, confidence explanation, limitations, and first-class counter-evidence. Missing repositories, partial discovery, unresolved identities, and conflicting evidence reduce confidence.

Distributed-monolith confirmation is reserved for strong explicit evidence such as unavoidable release and deployment lockstep combined with pervasive cross-repository coupling. Repository splits do not prove modularity. Central routing concentration does not prove ESB drift. Supplied Product Modules may be assessed structurally, but repositories are not treated as bounded contexts and semantic bounded-context inference is deferred.

Analyses and prior versions are retained without mutating repository evidence, dependency composition, or the canonical architecture graph. Typed lifecycle events are emitted at analysis, finding, and assessment boundaries rather than per indicator.

## Consequences

Users can inspect both modularity strengths and risks, trace findings to evidence, and understand mitigating characteristics. Classifications may remain insufficient or low confidence when evidence is incomplete. Product Architecture Scores, automated recommendations, proposed changes, live AI review, and automatic Review Board decisions remain deferred to later milestones.
