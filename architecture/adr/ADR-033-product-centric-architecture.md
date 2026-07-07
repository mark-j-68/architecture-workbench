# ADR-033 - Product-Centric Architecture

## Status

Accepted

## Context

Architecture Workbench currently models analysis primarily around a workspace
containing a repository. This is sufficient for Release 0.2 Discovery
Foundations, but it is not sufficient for enterprise software products.

Enterprise products increasingly consist of:

- multiple repositories
- multiple deployable units
- multiple bounded contexts
- multiple release streams
- multiple ownership teams
- shared libraries
- versioned and unversioned contracts
- several deployment environments

Repository-centric analysis can describe one codebase, but it cannot reliably
answer product-level architecture questions:

- Which capabilities does the product provide?
- Which bounded contexts exist?
- Which repositories and deployables must be released together?
- Which teams own which product modules?
- Which contracts connect the product?
- Is the product evolving toward healthy modularity or a distributed monolith?

## Decision

Product becomes the primary architectural abstraction for Release 0.3 and later.

Repositories become implementation artifacts and discovery sources. They remain
important, but they are no longer the canonical architecture boundary.

Architecture reasoning operates primarily at Product level. Product-level
analysis includes repositories, deployable units, bounded contexts,
capabilities, contracts, release streams, environments, ownership, policies,
integrations, relationships, recommendations, and scorecards.

The canonical hierarchy becomes:

```text
Enterprise
  Portfolio
    Product
      Repository
        Deployable Unit
          Bounded Context
            Component
```

This hierarchy is a reasoning structure, not a claim that implementation always
aligns cleanly. Mismatches between repository, deployable, bounded context, and
ownership boundaries are important discovery findings.

## Consequences

Architecture Workbench evolves from Repository Analysis into Product
Architecture Analysis.

Release 0.3 must compose Release 0.2 repository discovery outputs across
multiple repositories per product and interpret cross-repository relationships,
release dependencies, contracts, ownership, and distributed monolith smells.

Design Mode must support products that begin as a single repository but evolve
into multiple repositories and deployables.

Architecture scoring must operate at product level and explain modularity,
repository independence, release independence, deployment independence, bounded
context cohesion, ownership clarity, and distributed monolith risk.

The existing workspace concept remains useful as an initiative or working
container, but Product is the architecture subject under analysis.

## Boundaries

This ADR does not implement runtime changes.

It does not add:

- Java code
- API changes
- UI changes
- persistence changes
- event sourcing
- live AI providers

Future implementation milestones must update the canonical graph, discovery
engine, API, UI, and review workflows to treat Product as the primary
architecture boundary above Repository.
