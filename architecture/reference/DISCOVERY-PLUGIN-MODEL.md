# Discovery Plugin Model

The Discovery Engine is plugin-based. It must not become a monolithic scanner.
Plugins produce evidence and narrowly scoped observations for later
interpretation by the Architecture Intelligence Model.

## Core Concepts

DiscoveryPlugin: Executable discovery unit with a defined purpose, inputs,
outputs, dependencies, and failure policy.

DiscoveryPluginId: Stable identifier for a plugin, such as
`repository.discovery` or `java.structure`.

DiscoveryPluginMetadata: Name, version, category, supported technologies,
determinism mode, owner, and documentation link.

DiscoveryPluginCapability: Capability advertised by a plugin, such as
`detect-maven-modules`, `detect-spring-components`, or `extract-openapi`.

DiscoveryPluginDependency: Required or optional dependency on another plugin,
capability, or output type.

DiscoveryInput: Source references, prior plugin outputs, execution scope,
workspace/product context, and correlation metadata.

DiscoveryOutput: Evidence, observations, diagnostics, confidence values, and
proposed downstream inputs.

DiscoveryEvidence: Canonical evidence item with source, provenance, identity,
timestamps, references, confidence, and supporting artifacts.

DiscoveryObservation: Narrow structural observation directly supported by
evidence.

DiscoveryConfidence: Numeric value plus rationale, evidence coverage, and
confidence category.

DiscoveryExecutionContext: Actor, workspace/product id, source id, run id,
correlation id, causation id, configured storage, and plugin registry.

DiscoveryPluginResult: Plugin output wrapper containing status, evidence,
observations, diagnostics, elapsed time, and error details.

DiscoveryPluginStatus: `NOT_STARTED`, `SKIPPED`, `SUCCEEDED`,
`PARTIAL_SUCCESS`, `FAILED`.

## Plugin Definition Requirements

Every plugin must define:

- identity
- purpose
- supported technologies
- accepted inputs
- evidence produced
- observations produced
- confidence calculation
- dependencies on other plugins
- execution order constraints
- failure behaviour
- graph contribution policy
- AIM contribution policy

## Graph Contribution Policy

Discovery plugins must not mutate the canonical graph.

Allowed:

- produce evidence
- produce deterministic observations
- suggest graph-relevant facts for the intelligence layer
- provide inputs to proposed change generation

Not allowed:

- create graph elements directly
- create graph relationships directly
- accept proposed changes
- bypass validated graph services

## AIM Contribution Policy

Plugins may publish:

- Evidence
- narrowly scoped Observations
- confidence metadata
- provenance and diagnostics

Plugins must not publish:

- broad architecture findings
- product-level recommendations
- Review Board decisions
- accepted graph state

Findings and recommendations belong to the Architecture Intelligence Engine.

## Plugin Categories

### Source Plugin

Registers and validates sources such as local repositories, directories,
archives, future Git providers, future cloud inventories, or runtime sources.

### Build Plugin

Detects build systems, modules, artifacts, dependency declarations, and build
metadata.

### Language Plugin

Parses language-level constructs such as packages, classes, interfaces,
annotations, imports, and type references.

### Framework Plugin

Detects framework-specific components such as Spring controllers, services,
repositories, configuration, scheduled jobs, and application bootstraps.

### Dependency Plugin

Builds dependency relationships between modules, packages, classes,
repositories, libraries, and generated clients.

### Contract Plugin

Detects API, event, command, schema, and generated-client contracts.

### Messaging Plugin

Detects producers, consumers, topics, queues, event buses, broker references,
retry policies, and dead-letter configuration.

### Deployment Plugin

Detects deployable units, containers, infrastructure descriptors, deployment
manifests, and pipeline deployment metadata.

### Runtime Plugin

Future plugin category for runtime topology, observed traffic, cloud inventory,
metrics, traces, and deployment history.

Runtime plugins are out of scope for Release 0.2 implementation.

### AI-Assisted Interpretation Plugin

Optional future category for semantic interpretation, domain language grouping,
or ambiguous source classification.

AI-assisted plugins must be clearly separated from deterministic discovery
plugins. Their outputs must be labelled as AI-assisted inference, carry lower
default confidence unless corroborated, and never masquerade as observed facts.

## Dependency And Ordering Rules

Plugins declare dependencies as:

- required: plugin cannot run without dependency output
- optional: plugin can run with reduced confidence or reduced scope
- ordering-only: plugin should run after another but can tolerate absence

The orchestrator should compute execution order from dependencies and record
skipped plugins with reasons.

## Failure Behaviour

Plugin failures are isolated. A failed plugin should:

- return `FAILED` with diagnostics
- produce no invalid evidence
- preserve any previously emitted valid evidence only if it is complete and
  well-formed
- not cancel unrelated plugins
- reduce confidence for downstream observations that depended on it

## Determinism

Deterministic plugins should produce the same output for the same input source
and plugin version.

Heuristic plugins may infer candidates, but they must expose the heuristic and
confidence rationale.

AI-assisted plugins are probabilistic and must be optional, separately labelled,
and auditable.
