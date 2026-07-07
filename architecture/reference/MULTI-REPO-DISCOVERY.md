# Multi-Repo Discovery

Release 0.3 consumes Release 0.2 repository discovery outputs and interprets
them as Product Architecture Intelligence.

The question changes from "what did discovery observe inside this repository?"
to "how is this product implemented across repositories, deployables, contexts,
contracts, teams, and release streams?"

## Discovery Inputs

Multi-repo discovery should accept:

- a Product definition
- one or more repository locations
- optional ownership metadata
- optional pipeline metadata
- optional deployment metadata
- optional contract and API specification locations

Discovery remains local/static for Release 0.3 planning unless later milestones
explicitly add integrations.

## Repositories

Discovery should identify:

- repository name
- repository path or URL
- default branch where known
- build system
- language and framework signals
- docs and ADR presence
- ownership hints such as CODEOWNERS
- release and pipeline files

Repository evidence should map into Product architecture. Repository is not the
top-level architecture boundary.

## Maven Modules

Discovery should detect:

- root `pom.xml`
- child modules
- parent/child relationships
- artifact ids
- packaging type
- dependency management
- inter-module dependencies
- test module presence

Maven modules can imply product modules, deployable units, shared libraries, or
bounded context candidates.

## Gradle Modules

Discovery should detect:

- `settings.gradle` or `settings.gradle.kts`
- included builds
- subprojects
- Gradle plugins
- module dependencies
- application plugins
- library plugins

Gradle modules should be normalized to the same product architecture concepts
as Maven modules.

## Deployable Services

Discovery should identify deployable services through:

- Spring Boot application classes
- build plugin configuration
- Dockerfiles
- container image metadata
- deployment descriptors
- Helm charts
- Kubernetes manifests
- service-specific pipeline jobs

Deployable service evidence should map to Deployable Unit candidates.

## Lambda Functions

Discovery should identify serverless units through:

- AWS SAM templates
- Serverless Framework files
- Terraform resources
- handler classes
- deployment descriptors
- cloud function naming conventions

Lambda functions should map to Deployable Unit candidates and integration
relationships.

## Containers

Discovery should detect:

- Dockerfiles
- Compose files
- image names
- exposed ports
- entry points
- base images
- Kubernetes container specs
- Helm values

Containers provide evidence for deployables, deployment environments, and
operational complexity.

## Bounded Contexts

Discovery should infer bounded context candidates from:

- repository boundaries
- module boundaries
- package roots
- domain terms
- aggregate-like class names
- events and commands
- API resource names
- ownership boundaries
- deployment boundaries

Bounded context candidates remain proposed intelligence until reviewed and
accepted.

## Contracts

Discovery should find contracts such as:

- OpenAPI specifications
- GraphQL schemas
- gRPC protobuf files
- AsyncAPI specifications
- event schemas
- message classes
- command classes
- generated clients
- contract tests

Contracts should connect providers and consumers across product modules,
bounded contexts, and deployable units.

## Events

Discovery should detect:

- event classes
- event schema files
- broker topics
- producers
- consumers
- event naming conventions
- event versioning
- dead-letter and retry configuration

Events help identify asynchronous integrations and domain language.

## Commands

Discovery should detect:

- command classes
- command handlers
- message handlers
- API operations that represent commands
- workflow steps that issue commands

Commands help reveal orchestration, cross-context dependencies, and domain
responsibilities.

## Shared Libraries

Discovery should detect shared libraries through:

- dependency manifests
- internal group ids
- package names
- generated clients
- common DTO artifacts
- shared domain model artifacts
- shared utility modules

Shared libraries must be classified carefully. Utility libraries can be healthy.
Shared domain models across bounded contexts are a distributed monolith risk.

## Release Pipelines

Discovery should identify:

- CI/CD workflow files
- build jobs
- deployment jobs
- release tags
- versioning strategy
- artifact publication
- environment promotion
- manual approval gates

Pipelines provide evidence for release independence and lockstep release risk.

## Version Dependencies

Discovery should identify:

- library versions
- internal artifact versions
- generated client versions
- API contract versions
- event schema versions
- container image tags
- deployment chart versions

Version dependency analysis helps detect lockstep releases and contract
maturity.

## Ownership

Discovery should identify ownership from:

- CODEOWNERS
- repository metadata
- team catalogues
- service catalogues
- ADR authors
- pipeline owners
- package ownership conventions

Ownership should map to Ownership Team concepts and highlight mismatch between
ownership, repository boundaries, deployables, and bounded contexts.

## Repository Relationships

Discovery should detect relationships between repositories from:

- dependency manifests
- shared libraries
- generated clients
- contract references
- pipeline dependencies
- deployment ordering
- documentation links
- code references

Repository relationships are evidence for product-level coupling.

## Deployment Relationships

Discovery should detect:

- service-to-service runtime dependencies
- environment co-deployment
- shared infrastructure
- shared database usage
- deployment ordering
- runtime configuration references

Deployment relationships help assess deployment independence and operational
complexity.

## Communication Patterns

Discovery should infer:

- synchronous HTTP calls
- asynchronous messaging
- event publication and consumption
- shared database communication
- file/batch exchange
- orchestration workflows
- central routing patterns

Communication pattern analysis feeds distributed monolith detection and product
architecture scoring.

## Discovery Output

Multi-repo discovery should produce:

- evidence
- observations
- findings
- recommendations
- product module candidates
- bounded context candidates
- contract candidates
- proposed architecture changes
- architecture score inputs

Discovery must not directly mutate the canonical product graph. It proposes
changes for Review Board governance and explicit acceptance.
