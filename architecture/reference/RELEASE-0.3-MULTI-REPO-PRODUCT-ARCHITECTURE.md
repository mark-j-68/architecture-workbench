# Release 0.3 Multi-Repo Product Architecture Intelligence

Release 0.3 changes the Architecture Workbench ontology from repository-centric
analysis to product-centric architecture analysis.

Release 0.2 discovers evidence and deterministic structural observations from
repositories. Release 0.3 composes those discovery outputs across repositories
and interprets them at Product level.

## Why Repository-Centric Analysis Is Insufficient

A repository is a source control boundary. It is not always an architecture
boundary.

Enterprise products commonly span:

- multiple repositories
- multiple deployable units
- multiple bounded contexts
- multiple release streams
- multiple ownership teams
- shared libraries and shared contracts
- several runtime environments
- mixed communication styles

Repository-centric analysis answers questions such as "what is in this repo?"
It does not answer product-level architecture questions such as:

- Which capabilities does this product provide?
- Which bounded contexts own which behavior?
- Which repositories must be released together?
- Which services communicate synchronously or asynchronously?
- Which teams own the product architecture?
- Is this becoming a healthy modular product or a distributed monolith?

Treating a repository as the primary unit hides product structure. It can make
architecture look healthier than it is because coupling, lockstep releases, and
cross-repository dependencies are outside the single-repository view.

## Product-Centric Architecture

Release 0.3 makes Product the primary architectural unit above Repository.

A Product is a coherent business and technical system that delivers capabilities
to users or other systems. It may be implemented by one repository or many
repositories. It may contain one deployable unit or many deployable units. It
may start as a modular monolith and evolve into multiple services.

The product architecture view should explain:

- product capabilities
- product modules
- bounded contexts
- repository and deployable boundaries
- contracts between modules and contexts
- ownership and release streams
- deployment environments
- coupling and distributed monolith risk
- architecture score and recommendations

## Canonical Hierarchy

Architecture Workbench should reason over this hierarchy:

```text
Enterprise
  Portfolio
    Product
      Repository
        Deployable Unit
          Bounded Context
            Component
```

This hierarchy is conceptual rather than strictly physical. A bounded context
may span multiple deployable units during migration. A deployable unit may host
more than one bounded context in an immature system. A repository may contain
several deployable units. Product architecture analysis must make these
mismatches visible.

## Enterprise

An Enterprise is the organizational boundary containing portfolios, policies,
governance expectations, technology standards, and strategic constraints.

Release 0.3 does not implement enterprise portfolio intelligence, but it defines
the place where it will fit.

## Portfolio

A Portfolio groups related products by business domain, operating model,
investment stream, or strategic ownership.

Portfolio-level questions include:

- Which products duplicate capabilities?
- Which products share risky dependencies?
- Which products violate enterprise policies?
- Where should modernization investment go?

## Product

A Product is the primary architectural abstraction for Release 0.3.

Product-level questions include:

- What capabilities does the product provide?
- Which repositories implement the product?
- Which bounded contexts exist or should exist?
- Which deployables are independently releasable?
- Which contracts connect product modules?
- Which teams own architecture decisions?
- Is the product becoming more modular or more coupled?

## Repository

A Repository is an implementation boundary. It stores source code, build files,
tests, configuration, documentation, and infrastructure definitions.

Repository remains important, but it is no longer the primary architectural
boundary.

Repository is implementation evidence for product architecture. It helps
discover modules, deployables, packages, dependencies, tests, contracts, and
ownership signals. It does not define the product by itself.

## Deployable Unit

A Deployable Unit is something that can be built, versioned, released, and run:

- Spring Boot service
- worker
- batch job
- Lambda function
- container image
- frontend application
- shared runtime component

Deployable units are critical for release and operational independence.

## Bounded Context

A Bounded Context is a domain model boundary. It should own language, rules,
policies, commands, events, and decisions for a coherent part of the product.

Bounded contexts may align with repositories or deployables, but mismatches are
common and important to detect.

## Component

A Component is an internal building block within a deployable unit or bounded
context. Components support design and projection, but product architecture
reasoning should not stop at component diagrams.

## Repository As Implementation Boundary

Repository is now considered an implementation artifact because:

- a product can span many repositories
- one repository can contain multiple product modules
- one repository can contain multiple deployable units
- repository boundaries often reflect team history rather than domain design
- technical dependencies can cross repositories invisibly
- release coupling often appears only at product level
- shared libraries can create cross-context domain coupling

Architecture Workbench should use repositories as discovery sources, not as the
canonical product boundary.

## Release 0.3 Outcome

Release 0.3 should make Architecture Workbench capable of answering:

1. How do we analyze an existing product consisting of multiple repositories?
2. How do we design a new product that may begin as one repository and evolve
   into many?
3. How do we continuously assess whether a multi-repository product is evolving
   toward healthy modularity or a distributed monolith?
