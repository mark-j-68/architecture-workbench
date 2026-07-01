# ADR-026 - Thin API Shell over Architecture Kernel

## Status

Accepted

## Context

The Architecture OS kernel now has a proven in-memory workflow from workspace
creation, local discovery, evidence-backed findings, recommendations, proposed
changes, Review Board governance, explicit proposal acceptance, graph mutation,
projection generation, and immutable audit.

External callers need a minimal HTTP boundary, but the API must not become a
parallel domain model or a place where architecture business rules accumulate.

## Decision

Introduce `architecture-api` as a thin Spring Boot adapter over the Architecture
OS kernel.

The API exposes minimal endpoints for:

- creating and listing workspaces
- reading a workspace graph
- running local discovery
- listing discovery findings, recommendations, and proposed changes
- opening Review Board sessions
- recording Review Board votes
- closing Review Board sessions
- accepting, rejecting, and deferring proposed changes
- generating projections

Controllers map HTTP requests to API DTOs and delegate to an API facade. The
facade coordinates in-memory adapter state and calls existing kernel services:

- `WorkspaceService`
- `DiscoveryService`
- `ReviewBoardWorkflowService`
- `ProposedChangeService`
- `ProjectionService`

The API does not directly mutate `ArchitectureKnowledgeGraph`. Accepted graph
changes still pass through `ProposedChangeService`, which routes mutations
through validated graph services.

The API uses in-memory repositories and in-memory workflow state only.

## Consequences

The platform now has an HTTP shell for the kernel workflow without introducing
persistence, security, live AI providers, event sourcing, UI, or database
dependencies.

API DTOs remain separate from kernel domain objects, so future external API
evolution does not force changes into the domain model.

Future API expansion must preserve the same rule: controllers and API adapters
may orchestrate kernel services, but domain rules, validation, Review Board
governance, proposal acceptance, graph mutation, audit, and projection semantics
belong in the kernel and service modules.

## Boundaries

The API module is an adapter, not the source of business rules.

It must not bypass:

- validated graph application services
- `ProposedChangeService`
- `ReviewBoardWorkflowService`
- evidence-backed intelligence traceability
- typed architecture event emission
- immutable audit

Persistence, authentication, authorization, live provider calls, UI flows, and
event sourcing remain deliberately deferred.
