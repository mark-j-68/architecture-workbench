# Product Architecture Scorecard

The Release 0.3 Product Architecture Scorecard summarizes product-level
architecture health across multiple repositories, deployable units, bounded
contexts, contracts, release streams, and ownership teams.

The scorecard is an explainable decision aid. It is not a compliance grade.

## Metrics

### Product Modularity

Measures whether the product is decomposed into coherent modules aligned with
capabilities, bounded contexts, deployables, and ownership.

Signals:

- clear product module candidates
- low cross-module coupling
- module boundaries align with capabilities
- accepted architecture graph relationships are intentional

### Repository Independence

Measures whether repositories can evolve without constant coordinated source
changes.

Signals:

- low cross-repository source dependency
- no cyclic repository dependencies
- limited shared domain libraries
- contracts instead of shared implementation models

### Release Independence

Measures whether repositories and deployables can be released independently.

Signals:

- separate release streams
- versioned contracts
- independent pipeline execution
- low lockstep release evidence

### Deployment Independence

Measures whether deployable units can be deployed without unnecessary
coordination.

Signals:

- independent deployment pipelines
- no shared deployment gates for unrelated changes
- low runtime startup ordering dependency
- context-owned data and configuration

### Contract Maturity

Measures whether integrations are explicit, versioned, testable, and owned.

Signals:

- OpenAPI, AsyncAPI, protobuf, or schema evidence
- versioned contracts
- provider and consumer ownership
- contract tests
- deprecation policy

### Bounded Context Cohesion

Measures whether domain behavior, language, data, commands, and events are
coherent inside context boundaries.

Signals:

- aligned package/module/repository clusters
- consistent language
- context-owned commands and events
- low shared domain model use

### Coupling

Measures structural and runtime dependencies across modules, repositories,
deployables, and contexts.

Signals:

- package dependencies
- module dependencies
- repository dependencies
- shared libraries
- synchronous call graph
- shared database usage

Lower coupling increases score.

### Communication Complexity

Measures the number, type, and criticality of communication paths.

Signals:

- synchronous service calls
- event streams
- command messages
- central routers
- hidden orchestration
- fan-in and fan-out

Lower accidental complexity increases score.

### Ownership Clarity

Measures whether product elements have clear accountable teams.

Signals:

- CODEOWNERS
- team catalogue
- repository ownership
- deployable ownership
- contract ownership
- Review Board participants

### Operational Complexity

Measures the operational burden of the product architecture.

Signals:

- number of deployables
- environments
- pipelines
- runtime dependencies
- release coordination
- infrastructure complexity

Complexity is not automatically bad, but unexplained complexity reduces score.

### Architecture Drift

Measures divergence between intended product architecture and discovered
implementation.

Signals:

- graph elements without implementation evidence
- implementation evidence without graph representation
- stale ADRs
- changed dependencies
- new deployables or repositories not reflected in the model

### Distributed Monolith Risk

Measures whether multi-repo decomposition is creating distributed coupling
rather than healthy modularity.

Signals:

- release lockstep
- cyclic repository dependencies
- shared database
- shared domain model libraries
- hidden orchestration
- cross-context transactions
- excessive synchronous communication

### Overall Product Architecture Score

The overall score combines the metric scores into one explainable value.

Recommended weighting for Release 0.3 planning:

- Product modularity: 15 percent
- Repository independence: 10 percent
- Release independence: 10 percent
- Deployment independence: 10 percent
- Contract maturity: 10 percent
- Bounded context cohesion: 10 percent
- Coupling: 10 percent
- Communication complexity: 5 percent
- Ownership clarity: 5 percent
- Operational complexity: 5 percent
- Architecture drift: 5 percent
- Distributed monolith risk: 5 percent

The score should show contributing evidence and should not hide low-confidence
inputs behind a precise number.

## Confidence Calculation

Each metric should have a confidence score separate from the metric score.

Confidence should increase when:

- multiple independent evidence sources agree
- evidence is recent
- evidence is directly observed from source files, build files, pipelines, or
  contracts
- ownership metadata is explicit
- dependency relationships are machine-detected
- findings are reviewed or accepted by the Review Board

Confidence should decrease when:

- evidence is inferred only from naming
- repositories are missing from product discovery
- pipeline or runtime metadata is unavailable
- ownership is unknown
- contracts are undocumented
- discovery is partial or stale

Suggested confidence formula:

```text
confidence =
  evidence_coverage * 0.35 +
  evidence_quality * 0.25 +
  recency * 0.15 +
  corroboration * 0.15 +
  review_validation * 0.10
```

Each factor is normalized from 0.0 to 1.0.

## Output Shape

The scorecard should produce:

- metric name
- score
- confidence
- evidence references
- findings
- recommendations
- trend if previous scorecards exist
- Review Board status where relevant

## Governance

Low scores do not automatically mutate the graph or create decisions. They
produce findings, recommendations, proposed changes, and evidence packs for
Review Board workflow.
