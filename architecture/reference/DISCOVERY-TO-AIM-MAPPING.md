# Discovery To AIM Mapping

Discovery output enters the Architecture Intelligence Model through the
evidence-first traceability chain.

Canonical flow:

```text
Discovered Source
-> Evidence
-> Observation
-> Finding
-> Recommendation
-> Proposed Change
-> Review Board
-> Explicit Acceptance
-> Graph Mutation
```

## Responsibilities

Discovery plugins produce:

- evidence
- narrowly scoped deterministic observations
- confidence values
- provenance
- diagnostics

Architecture Intelligence produces:

- findings
- recommendations
- architecture scores
- risk interpretation
- recommendation candidates

Proposed Change workflow produces:

- proposed element additions
- proposed relationship additions
- accepted/rejected/deferred status

Review Board produces:

- session decisions
- votes
- governance traceability

Knowledge Graph records:

- accepted architectural state only

## Evidence Mapping

Each DiscoveryEvidence item maps to AIM Evidence.

Required mapping:

- id: stable evidence id
- source: discovery source and plugin id
- provenance: file path, parser, source reference, checksum, timestamp
- confidence: evidence confidence
- timestamp: discovery timestamp
- references: source paths, coordinates, symbols, contract ids
- supportingArtifacts: files, snippets, schema references, build descriptors

## Observation Mapping

Deterministic DiscoveryObservation maps to AIM Observation when:

- it is directly supported by evidence
- its inference rule is named
- confidence is calculated
- related evidence ids are present
- related graph elements are optional and not assumed

Examples:

- Maven module detected
- Spring controller detected
- endpoint route detected
- package dependency detected
- Dockerfile detected

## Finding Boundary

Architectural findings belong to the intelligence layer.

Discovery should not directly create findings such as:

- "this is a distributed monolith"
- "bounded context is wrong"
- "architecture style is unhealthy"
- "team should split this repository"

Discovery may publish observations that AIM later interprets into findings.

## Recommendation Boundary

Recommendations belong to the intelligence layer.

Discovery should not directly recommend:

- split a service
- merge modules
- create a bounded context
- replace shared domain model
- change release strategy

Discovery may provide evidence needed for those recommendations.

## Graph Mutation Boundary

Discovery must not directly mutate the canonical graph.

Allowed path:

```text
Evidence -> Observation -> Finding -> Recommendation -> Proposed Change
```

Only accepted proposed changes may mutate the graph through validated
application services.

## Release 0.3 Reuse

Release 0.3 consumes multiple Release 0.2 discovery outputs and interprets them
at Product level.

Release 0.2 produces:

- repository evidence
- structural observations
- confidence metadata

Release 0.3 interprets:

- product modules
- repository relationships
- release independence
- contract maturity
- product modularity
- distributed monolith risk

This separation keeps discovery reusable across single-repository,
multi-repository, runtime, cloud, and future AI-assisted sources.
