# Release 0.2 Discovery Engine

Release 0.2 defines the Discovery Engine: a pluggable, evidence-first
foundation for inspecting software systems and publishing traceable
architectural facts into the Architecture Intelligence Model.

## Purpose

The Discovery Engine observes software systems. It collects evidence and creates
deterministic structural observations that later reasoning layers can interpret.

Release 0.2 supports:

- single repository discovery
- future multi-repository discovery
- multiple technology stacks
- deterministic discovery plugins
- future AI-assisted discovery plugins
- evidence provenance
- confidence calculation
- incremental discovery
- plugin dependencies
- traceability into AIM
- future runtime and cloud discovery

## Boundary

Discovery Engine:

- collects evidence
- normalizes evidence
- records provenance
- calculates confidence for evidence and narrow observations
- emits deterministic structural observations
- publishes evidence and observations into AIM

Architecture Intelligence Engine:

- interprets observations
- creates findings
- proposes recommendations
- identifies architecture risks
- reasons about modularity, coupling, contracts, and product architecture
- prepares proposed changes

Review Board:

- governs recommendations and proposed changes
- records votes and decisions
- does not automatically mutate the graph

Knowledge Graph:

- records accepted architectural state
- changes only through validated services and accepted proposed changes

## Release 0.2 Versus Release 0.3

Release 0.2 is Discovery Foundations and Evidence Collection.

It answers:

- What repositories, files, modules, classes, packages, contracts, messages, and
  deployment descriptors exist?
- What evidence proves those facts?
- What narrow structural observations can be made deterministically?
- How confident are those observations?

Release 0.3 is Multi-Repo Product Architecture Intelligence.

It answers:

- How do multiple repository discoveries compose into a product?
- Is the product modular?
- Are repositories, deployables, releases, and contracts independent?
- Is the product drifting toward a distributed monolith?

## Non-Goals For v0.2

Release 0.2 does not:

- perform high-level product architecture judgement
- calculate distributed monolith risk
- decide bounded contexts definitively
- decide architecture style definitively
- create recommendations directly inside scanner plugins
- mutate the canonical graph directly
- call live AI providers
- perform cloud runtime scanning
- require database persistence
- introduce event sourcing
- implement GitHub PR integration
- replace Review Board governance

## Architectural Principle

The Discovery Engine observes.

The Architecture Intelligence Engine interprets.

The Review Board governs.

The Knowledge Graph records accepted architectural state.
