# Product Architecture Meta-Model

This document proposes the canonical concepts required for Release 0.3:
Multi-Repo Product Architecture.

The model extends Architecture Workbench beyond repository analysis. Product is
the primary architecture boundary. Repository becomes an implementation
artifact and discovery source.

## Enterprise

Purpose: Represents the organization or business boundary in which portfolios
and products exist.

Ownership: Enterprise architecture, technology leadership, governance, and
business leadership.

Relationships: Owns portfolios, policies, standards, strategic constraints, and
enterprise-wide risks.

Lifecycle: Created when an organization is onboarded; evolves as portfolios,
policies, and governance structures change.

Traceability: Traces to enterprise policies, portfolio decisions, standards,
regulatory evidence, and architecture review outcomes.

## Portfolio

Purpose: Groups related products by business domain, investment stream,
operating model, or ownership structure.

Ownership: Portfolio architecture, product leadership, business sponsors, and
domain leadership.

Relationships: Belongs to an enterprise; owns products; may define portfolio
roadmaps, shared capabilities, and modernization priorities.

Lifecycle: Created around a product family or domain; evolves as products are
created, merged, retired, or replatformed.

Traceability: Traces to portfolio goals, product strategies, investment cases,
capability maps, risks, and review decisions.

## Product

Purpose: Primary architectural abstraction for Release 0.3. Represents a
coherent business and technical system delivering capabilities.

Ownership: Product owner, product architect, engineering lead, and ownership
teams.

Relationships: Belongs to a portfolio; contains product modules, repositories,
deployable units, bounded contexts, capabilities, contracts, release streams,
environments, policies, integrations, and relationships.

Lifecycle: Created for a greenfield product or discovered from existing
repositories; evolves through design, discovery, releases, reviews, migrations,
and retirement.

Traceability: Traces to capabilities, repository evidence, architecture
decisions, contracts, findings, recommendations, proposed changes, Review Board
decisions, and audit events.

## Product Module

Purpose: Logical architecture partition within a product. A product module may
map to a repository, deployable unit, package cluster, bounded context, or
business capability.

Ownership: Product architect and the team accountable for the module.

Relationships: Belongs to a product; may contain or align with bounded
contexts, deployable units, capabilities, contracts, and repositories.

Lifecycle: Proposed during design or inferred during discovery; confirmed,
merged, split, or retired through Review Board decisions.

Traceability: Traces to evidence, inferred boundaries, findings,
recommendations, accepted proposed changes, and product decisions.

## Repository

Purpose: Source control and implementation boundary containing code, build
configuration, documentation, tests, infrastructure definitions, and CI/CD
metadata.

Ownership: Engineering team or platform team responsible for source changes and
release mechanics.

Relationships: Belongs to a product; may contain modules, deployable units,
contracts, libraries, and implementation components.

Lifecycle: Created, imported, split, merged, archived, or replaced as product
implementation evolves.

Traceability: Traces to discovered files, commits, build metadata, ownership
metadata, release pipelines, dependency manifests, and evidence.

## Deployable Unit

Purpose: Buildable and releasable runtime unit such as a service, worker,
Lambda, container, frontend, batch job, or scheduled process.

Ownership: Team responsible for build, release, runtime operations, and service
quality.

Relationships: Belongs to a product and usually to a repository; may implement
one or more bounded contexts; exposes or consumes contracts.

Lifecycle: Designed or discovered; versioned, deployed, scaled, retired, or
split as runtime architecture evolves.

Traceability: Traces to build files, Dockerfiles, deployment descriptors,
runtime configuration, pipeline evidence, contracts, and operational findings.

## Bounded Context

Purpose: Domain model boundary with its own language, rules, policies, commands,
events, and decisions.

Ownership: Domain-aligned team with product architecture oversight.

Relationships: Belongs to a product; may be implemented by one or more
deployable units and repositories; owns capabilities, contracts, commands,
events, policies, and integrations.

Lifecycle: Proposed during design or inferred during discovery; refined through
DDD validation, review, split/merge decisions, and product evolution.

Traceability: Traces to domain evidence, package/module clusters, ubiquitous
language, commands, events, policies, ADRs, findings, and Review Board
decisions.

## Capability

Purpose: Business ability the product provides, independent of implementation
shape.

Ownership: Product management and domain leadership, with architecture
accountability for realization.

Relationships: Belongs to a product or portfolio; is realized by product
modules, bounded contexts, deployable units, and contracts.

Lifecycle: Defined during product design or discovered from existing behavior;
evolves with product strategy.

Traceability: Traces to business goals, requirements, evidence, bounded
contexts, contracts, decisions, and generated projections.

## Contract

Purpose: Abstract agreement between product modules, bounded contexts,
deployable units, or external systems.

Ownership: Provider and consumer teams, with product architecture governance.

Relationships: Generalizes API Contract, Event Contract, and Command Contract;
connects providers and consumers.

Lifecycle: Proposed, versioned, published, consumed, deprecated, and retired.

Traceability: Traces to source definitions, generated specs, tests, runtime
evidence, decisions, compatibility findings, and consumers.

## API Contract

Purpose: Synchronous request/response contract such as REST, GraphQL, gRPC, or
internal HTTP API.

Ownership: Provider team owns published contract; consumers own integration
usage; product architecture owns compatibility expectations.

Relationships: Specializes Contract; connects deployable units, bounded
contexts, and external systems.

Lifecycle: Designed or discovered; versioned, tested, published, deprecated,
and retired.

Traceability: Traces to OpenAPI specs, controller code, generated clients,
contract tests, gateway config, and review findings.

## Event Contract

Purpose: Asynchronous fact contract emitted by a bounded context or deployable
unit.

Ownership: Producing context owns event meaning and schema; consumers own
reaction behavior.

Relationships: Specializes Contract; relates to events, policies, integration
relationships, topics, streams, and consumers.

Lifecycle: Designed or discovered; versioned, published, consumed, deprecated,
and retired.

Traceability: Traces to event classes, schemas, topics, broker configuration,
consumers, tests, and compatibility decisions.

## Command Contract

Purpose: Intent contract that requests behavior from another context or
deployable unit.

Ownership: Provider owns command handling contract; consumer owns command use.

Relationships: Specializes Contract; relates to commands, policies, APIs,
message handlers, and orchestration flows.

Lifecycle: Designed or discovered; versioned, validated, handled, deprecated,
and retired.

Traceability: Traces to command classes, handlers, message schemas, API
operations, workflow definitions, and findings.

## Release Stream

Purpose: Represents the cadence, versioning, and coordination pattern by which
product modules, repositories, or deployables are released.

Ownership: Engineering teams, release management, product leadership, and
platform operations.

Relationships: Connects repositories, deployable units, environments, version
dependencies, contracts, and ownership teams.

Lifecycle: Created with product delivery model; evolves as release independence
improves or coupling increases.

Traceability: Traces to pipelines, tags, release notes, deployment history,
dependency versions, and lockstep-release findings.

## Deployment Environment

Purpose: Runtime environment where deployable units are promoted and operated.

Ownership: Platform, operations, SRE, and product teams.

Relationships: Hosts deployable units; connects to release streams, policies,
configuration, integrations, and operational evidence.

Lifecycle: Created for dev/test/stage/prod or specialized runtime needs;
retired or replaced as platform strategy changes.

Traceability: Traces to infrastructure definitions, deployment records, runtime
configuration, policies, and operational findings.

## Ownership Team

Purpose: Human accountability boundary for product modules, repositories,
deployables, contracts, and decisions.

Ownership: Engineering management, product leadership, and team leads.

Relationships: Owns or stewards products, modules, repositories, deployables,
bounded contexts, contracts, policies, and risks.

Lifecycle: Created from organization data or inferred from repo metadata;
changes through reorganizations and ownership transfers.

Traceability: Traces to CODEOWNERS, repository permissions, team catalogues,
incident ownership, ADR authorship, and review participation.

## Policy

Purpose: Governance rule or constraint applied to product architecture.

Ownership: Enterprise architecture, security, compliance, platform, and product
architecture.

Relationships: Applies to products, repositories, deployable units, contracts,
environments, and ownership teams.

Lifecycle: Defined, approved, applied, reviewed, updated, waived, or retired.

Traceability: Traces to standards, regulations, control evidence, findings,
exceptions, and audit events.

## Integration

Purpose: Concrete communication or dependency between product architecture
elements.

Ownership: Provider and consumer owners, with product architecture governance.

Relationships: Connects products, modules, repositories, deployables, bounded
contexts, contracts, environments, and external systems.

Lifecycle: Designed or discovered; versioned, monitored, reviewed, deprecated,
and retired.

Traceability: Traces to contracts, code references, dependency manifests,
runtime configuration, broker topics, API gateways, and findings.

## Relationship

Purpose: General graph edge between product architecture elements.

Ownership: Depends on relationship type; product architecture owns canonical
meaning and validation.

Relationships: Connects any two canonical elements with a typed semantic.

Lifecycle: Proposed, accepted, updated, deprecated, or removed through
validated services and governance.

Traceability: Traces to evidence, recommendations, proposed changes, review
decisions, and audit events.

## Core Invariant

Product is the canonical architectural boundary above Repository. Repository is
evidence and implementation detail. Architecture reasoning, scoring,
recommendations, governance, and continuous assessment operate primarily at
Product level.
