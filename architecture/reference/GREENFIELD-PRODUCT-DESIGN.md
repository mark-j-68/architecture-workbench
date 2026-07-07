# Greenfield Product Design

Release 0.3 must support product architecture design as well as discovery.

The architect should be able to design a product that may start as one
repository and evolve into many repositories without losing architecture
intent.

## Design Goal

Architecture Workbench should help architects model the Product first, then
choose repository, deployable, and ownership boundaries as implementation
decisions.

## Product

The architect defines:

- product name
- product purpose
- portfolio
- target users or consumers
- business outcomes
- architecture constraints
- lifecycle stage

Product is the primary design boundary.

## Capabilities

The architect defines product capabilities before repositories.

Each capability should include:

- name
- description
- owner
- business priority
- supporting bounded contexts
- contracts
- evidence or assumptions
- planned maturity

Capabilities provide the bridge between business architecture and technical
architecture.

## Bounded Contexts

The architect defines bounded contexts from domain language and business
responsibility.

Each bounded context should include:

- name
- ubiquitous language
- owned capabilities
- commands
- events
- policies
- data ownership
- integration contracts
- ownership team

Bounded contexts should not be forced to match repositories prematurely.

## Repositories

The architect defines initial repository strategy:

- single repository
- modular monorepo
- product repo plus libraries
- multiple service repositories
- context-aligned repositories
- generated contract repository

Repositories are implementation boundaries. They should be justified by team
ownership, release independence, build constraints, or operational needs.

## Deployment Units

The architect defines deployable units such as:

- modular monolith
- Spring Boot service
- frontend application
- worker
- batch process
- Lambda function
- containerized adapter

Deployable units should align with runtime and release needs, not just source
layout.

## Sales Packaging

The architect can model how product capabilities are packaged for customers or
internal users.

Examples:

- core product
- premium module
- regional variant
- regulated-market edition
- platform capability
- internal shared service

Sales packaging may differ from repository and deployable boundaries.

## Versioning Strategy

The architect defines how the product and its parts are versioned:

- product version
- repository version
- deployable version
- contract version
- event schema version
- compatibility policy
- deprecation policy

Versioning strategy is essential for avoiding release lockstep.

## Contract Strategy

The architect defines how product modules communicate:

- API contracts
- event contracts
- command contracts
- schema ownership
- consumer compatibility
- contract tests
- published documentation
- backward compatibility rules

Contract strategy should be defined before splitting deployables or
repositories.

## Ownership

The architect defines:

- product owner
- product architect
- bounded context owners
- repository owners
- deployable owners
- contract owners
- Review Board participants

Ownership should align with product modules and bounded contexts where
possible.

## Scaling Strategy

The architect defines how the product can scale:

- team scaling
- repository scaling
- deployable scaling
- data ownership scaling
- operational scaling
- contract governance scaling
- product module evolution

Scaling decisions should be recorded as evidence-backed decisions, not hidden
inside repository structure.

## Migration Path From One Repository To Many

Architecture Workbench should help design an intentional migration path:

1. Start with Product and capability model.
2. Define bounded contexts and product modules.
3. Implement as one repository if that is fastest and safe.
4. Keep internal module boundaries explicit.
5. Define contracts before physical split.
6. Track coupling, cycles, and ownership.
7. Use architecture scorecard to identify split candidates.
8. Propose repository or deployable split as governed architecture change.
9. Review through Review Board.
10. Accept and update product graph when the split is approved.

## Greenfield Invariants

- Product comes before Repository.
- Repository strategy is an implementation decision.
- Deployable strategy is a runtime decision.
- Bounded context strategy is a domain decision.
- Contract strategy protects future modularity.
- Ownership must be explicit.
- Migration from one repo to many must preserve traceability.
