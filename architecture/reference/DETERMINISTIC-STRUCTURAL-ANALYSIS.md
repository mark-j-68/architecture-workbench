# Deterministic Structural Analysis

## Purpose

Release 0.2.5 derives explainable structural observations from discovery evidence. It does not interpret those observations as architectural findings. Every result remains traceable to the evidence that supported its derivation and is available through the existing discovery result/domain boundary.

Release 0.2 collects evidence and derives deterministic or narrowly inferred structural observations. Release 0.3 may interpret those observations at Product level and produce findings, risks, scorecards, or recommendations.

## Plugins

### PackageCycleAnalysisPlugin

`analysis.package-cycle` consumes internal `package-dependency` evidence. It calculates strongly connected components and emits direct or multi-package cycle observations, ordered members, all dependency edges within the component, crossed module boundaries, and exact edge sequences. A cycle is reported as structure, not as a violation.

### ModuleDependencyAnalysisPlugin

`analysis.module-dependency` compares internal Maven artifact declarations with source-level module package references. It reports declared and observed directions, declarations without detected use, source use without a matched declaration, bidirectional references, and distinct module fan-in/fan-out. Absence observations use high rather than absolute confidence because discovery input may be incomplete.

### LayerStructureAnalysisPlugin

`analysis.layer-structure` identifies candidate controller, service, repository, domain/entity, configuration, adapter, and port layers. Explicit Spring markers have 100% confidence as role evidence. Package-name conventions have 70% confidence and always use candidate-layer wording. Dependency direction observations preserve relationships such as controller-to-service, service-to-repository, repository-to-entity, controller-to-repository, repository-to-controller, and controller transaction boundaries. They are not labelled violations.

### ComponentDependencyAnalysisPlugin

`analysis.component-dependency` joins constructor, field, setter, interface, import, and Spring component evidence into component edges. It emits fan-in, fan-out, bidirectional dependency, exact dependency path, and highly connected component count observations. “Highly connected” is a numeric threshold observation only; it is not a God-service or design-quality classification.

### ContractVersionAnalysisPlugin

`analysis.contract-version` reports explicit version presence, absence, multiple versions, deprecation, producer and consumer version references, exact literal matches, exact literal mismatches, and unresolved comparisons. Dynamic or missing values are unresolved. Even an exact mismatch remains an observation and produces no compatibility-risk finding or recommendation.

### MessagingTopologyAnalysisPlugin

`analysis.messaging-topology` calculates producer, consumer, and channel counts; channel fan-in and fan-out; event-to-command paths and hop counts; route participation; structurally central routing nodes; consumers without detected producers; channels without detected consumers; and dead-letter, retry, archive/replay, and fan-out configuration presence. Centrality is reported numerically and is not classified as ESB drift or a smell.

### DependencyMetricsPlugin

`analysis.dependency-metrics` emits `StructuralMetric` values for:

- packages, Java types, Maven modules, dependency edges, and cycles;
- average and maximum package fan-in/fan-out;
- component dependencies;
- contracts, channels, producers, and consumers;
- explicit contract-version coverage percentage;
- test-source, ADR, and documentation presence.

Metrics retain supporting evidence identifiers and map to AIM Evidence, Observation, and Metric structures. Trend direction is `UNKNOWN` for a single discovery run. No overall architecture score is calculated.

## Typed analysis model

The discovery model uses value objects rather than untyped graph maps:

- `DependencyNode`, `DependencyEdge`, `DependencyCycle`, and `DependencyPath`;
- `StructuralMetric` and `LayerCandidate`;
- `VersionComparison`;
- `TopologyNode`, `TopologyEdge`, and `TopologyPath`.

These are Release 0.2 discovery concepts. They do not introduce Product, ProductModule, ReleaseStream, or other Release 0.3 ontology.

## Provenance and explainability

Each analysis evidence item includes stable plugin and evidence identifiers, absolute workspace and repository context, observed or inferred classification, confidence and rationale, sorted supporting evidence identifiers, an explanation/derivation summary, and truncation status. Cycle and route results include exact edge sequences.

The companion `DiscoveryObservation` refers to both the analysis evidence and its source evidence. Discovery-to-AIM mapping therefore preserves the evidence-first traceability chain.

## Confidence rules

| Analysis | Default confidence |
| --- | --- |
| Cycle membership from explicit internal package edges | 100% |
| Literal version presence, match, mismatch, or deprecation | 100% |
| Counts over supplied evidence | 100% for the supplied evidence set |
| Explicit Spring role used as a candidate layer | 100% role evidence |
| Dependency direction joined to candidate roles | 90% |
| Missing declaration/use or topology counterpart | 90%, acknowledging incomplete input |
| Package naming as a candidate layer | 70% |
| Dynamic or absent version comparison | 70% unresolved |

Confidence describes the derivation from supplied evidence, not completeness of the repository or correctness of an architectural interpretation.

## Scale protection and partial success

Graph analysis is bounded by 10,000 nodes, 50,000 edges, traversal depth 32, and 5,000 emitted topology paths. Traversal tracks visited nodes, handles disconnected graphs, and cannot loop indefinitely. Reaching a limit adds a diagnostic, marks relevant output as truncated where applicable, and returns `PARTIAL_SUCCESS` while preserving complete results found before the limit.

## Architectural interpretation boundary

Release 0.2.5 does not create findings, recommendations, proposed changes, Review Board sessions, scorecards, or canonical graph mutations. In particular it does not classify distributed monoliths, bounded contexts, God services, poor layering, unhealthy coupling, ESB drift, or router smells.

Release 0.3 may consume the evidence and observations through the Architecture Intelligence Engine. Any inferred canonical graph addition must still follow the Proposed Change and explicit acceptance workflow.

## Known limitations

- Maven declaration matching is limited to internal artifacts that can be matched by literal artifact id.
- Source and topology absence means “not detected in supplied evidence,” not proof of runtime absence.
- Candidate layers are technical labels and do not imply domain boundaries.
- Version comparison only declares mismatch when both sides expose incompatible literal values; it does not implement semantic compatibility rules.
- Path analysis operates on statically discovered topology and does not execute code or contact brokers.
