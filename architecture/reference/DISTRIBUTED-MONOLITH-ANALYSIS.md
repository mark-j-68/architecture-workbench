# Distributed Monolith Analysis

Release 0.3 must help teams understand whether a multi-repository product is
evolving toward healthy modularity or a distributed monolith.

A distributed monolith is a system split across repositories, services, or
deployables but still coupled in release, data, domain model, ownership, or
runtime behavior.

## Smell Catalogue

### Repository Coupling

Description: Repositories depend heavily on each other through source
dependencies, generated clients, shared libraries, or coordinated changes.

Evidence: Internal dependency manifests, cross-repo imports, generated clients,
shared build versions, linked pull requests, or documentation requiring
coordinated changes.

Severity: Medium to high, depending on whether coupling blocks independent
release.

Confidence: Increases with multiple evidence types across dependencies,
pipelines, and release history.

Recommendations: Identify product modules, define explicit contracts, reduce
source coupling, and separate domain from utility dependencies.

### Release Lockstep

Description: Multiple repositories or deployables must be released together for
ordinary product changes.

Evidence: Shared release tags, pipeline dependencies, deployment ordering,
manual release checklists, synchronized version bumps, or linked change tickets.

Severity: High when independent deployability is a product goal.

Confidence: High when pipeline and version evidence both show coordinated
release.

Recommendations: Version contracts, decouple deployment units, introduce
backward-compatible changes, and track release independence.

### Shared Libraries Becoming Shared Domain Model

Description: A shared library contains domain concepts, entities, commands,
events, or DTOs used across bounded contexts.

Evidence: Internal shared artifacts with domain package names, shared entity
classes, shared command/event schemas, or common model dependencies across
contexts.

Severity: High for DDD-oriented products.

Confidence: High when shared classes carry domain language and are consumed by
multiple contexts.

Recommendations: Split shared utility from shared domain, move ownership to
bounded contexts, publish versioned contracts instead of shared models.

### Cross-Context Transactions

Description: Business workflows require transactional consistency across
bounded contexts or deployables.

Evidence: Distributed transaction libraries, synchronous write chains, shared
transaction identifiers, compensating logic hidden in services, or database
transactions spanning modules.

Severity: High.

Confidence: Medium from code evidence; high when runtime or transaction
configuration confirms it.

Recommendations: Revisit aggregate boundaries, introduce eventual consistency,
publish domain events, and review workflow ownership.

### Unversioned Contracts

Description: APIs, events, or commands are used across boundaries without clear
versioning or compatibility policy.

Evidence: OpenAPI files with no version, event schemas without versions,
generated clients tied to latest, undocumented message formats, or breaking
changes in shared DTOs.

Severity: Medium to high.

Confidence: High when contract files exist and lack version fields or when
consumer references use unpinned versions.

Recommendations: Introduce semantic or compatibility versioning, contract
tests, deprecation policy, and provider/consumer ownership.

### Central Event Router Becoming ESB

Description: A central router, broker service, or integration layer owns too
much routing, transformation, and business process behavior.

Evidence: Central message processor, generic event router, transformation
rules, topic fan-out controlled by one team, or orchestration logic outside
domain contexts.

Severity: High when business behavior is centralized.

Confidence: Medium from topology; high when transformation and business rules
are visible in the router.

Recommendations: Move behavior to owning contexts, keep broker/router
infrastructure generic, and review event ownership.

### Shared Database

Description: Multiple bounded contexts, deployables, or repositories read and
write the same database schema.

Evidence: shared JDBC URLs, common schema migrations, cross-service table
access, shared ORM entities, or deployment configs pointing to one database.

Severity: High.

Confidence: High when configuration and migration evidence agree.

Recommendations: Define data ownership, introduce context-owned schemas,
publish events or APIs for cross-context access, and plan migration seams.

### Shared DTO Explosion

Description: Many shared DTOs are used as integration contracts and internal
domain objects across boundaries.

Evidence: large common DTO packages, generated client models reused internally,
high DTO churn, or DTO dependencies across multiple repositories.

Severity: Medium.

Confidence: Increases with DTO count, usage breadth, and churn.

Recommendations: Separate API DTOs from domain models, version external
contracts, and reduce shared DTO packages.

### Cyclic Repository Dependencies

Description: Repositories depend on each other directly or indirectly in a
cycle.

Evidence: dependency manifests, generated clients, build ordering, or shared
artifact dependencies forming cycles.

Severity: High.

Confidence: High when dependency graph analysis detects a cycle.

Recommendations: Break cycles with contracts, invert dependencies, extract
stable APIs, or merge/split modules deliberately.

### Package Ownership Mismatch

Description: Package or module ownership does not align with repository,
bounded context, or team ownership.

Evidence: CODEOWNERS mismatch, package naming conflicts, team catalogue
mismatch, or frequent changes by teams outside the nominal owner.

Severity: Medium.

Confidence: Medium from metadata; high with ownership and commit evidence.

Recommendations: Clarify ownership, align packages with contexts, and document
module stewardship.

### Hidden Orchestration

Description: Workflow orchestration is hidden inside services, scripts,
pipelines, or integration code rather than explicit product architecture.

Evidence: service classes invoking many contexts, pipeline scripts coordinating
business steps, batch jobs with domain decisions, or integration glue code.

Severity: Medium to high.

Confidence: Medium from static code; high when orchestration paths are visible.

Recommendations: Model workflows explicitly, identify command/event ownership,
and move orchestration to deliberate process boundaries.

### Excessive Synchronous Communication

Description: Product behavior depends on long synchronous call chains across
deployables or bounded contexts.

Evidence: HTTP clients between services, API gateway routing chains, timeout
configurations, circuit breaker sprawl, or service dependency graphs.

Severity: Medium to high depending on critical path.

Confidence: Medium from code; high with runtime topology or tracing.

Recommendations: Shorten call chains, introduce asynchronous events where
appropriate, and define ownership of process steps.

### Duplicated Ubiquitous Language

Description: Different contexts use the same terms for different meanings, or
different terms for the same concept without explicit translation.

Evidence: duplicated entity names, conflicting package terms, inconsistent API
resources, or repeated domain vocabulary across repositories.

Severity: Medium.

Confidence: Medium from naming; high when paired with bounded context evidence.

Recommendations: run language discovery, define context maps, document
translations, and refine bounded contexts.

### Capability Fragmentation

Description: One business capability is scattered across many repositories,
teams, or deployables without a clear owning module or context.

Evidence: feature code spread across repositories, multiple services owning
parts of one capability, release lockstep for a capability, or unclear team
ownership.

Severity: High when delivery speed or reliability suffers.

Confidence: Medium from code layout; high when ownership and release evidence
confirm fragmentation.

Recommendations: define capability ownership, group related product modules,
review bounded context boundaries, and reduce cross-team dependencies.

## Scoring Use

Distributed monolith smells should feed:

- findings
- confidence scores
- architecture recommendations
- product architecture scorecard
- proposed architecture changes
- Review Board evidence packs

No smell should automatically mutate the graph. Smells produce evidence-backed
findings and recommendations.
