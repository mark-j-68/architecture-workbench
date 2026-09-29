# North Star Demo 0.3

Release 0.3 demonstrates Multi-Repo Product Architecture Intelligence.

It consumes Release 0.2 repository discovery outputs and reasons across them at
Product level.

The demo length is 20 minutes.

## Demo Promise

"Architecture Workbench now understands a product, not just a repository. We
can attach several repositories, discover how the product is actually built,
define Product Modules, identify bounded-context candidates, detect contracts and release dependencies,
score architecture health, identify distributed monolith risk, and govern a
recommended change through the Review Board."

## Demo Product

Use a sample product with four repositories:

1. `customer-api`
2. `origination-service`
3. `pricing-service`
4. `shared-domain-model`

The sample should include:

- Maven or Gradle builds
- Spring Boot services
- API contracts
- event or command classes
- shared library dependencies
- pipeline files
- ownership hints
- at least one distributed monolith smell

## 20-Minute Flow

### 0:00-2:00 - Create Product

Action:

- create Product: `Mortgage Origination Platform`
- assign Portfolio: `Retail Lending`
- define initial capabilities:
  - Customer Intake
  - Loan Origination
  - Pricing
  - Decisioning

Expected state:

- Product is the top-level architecture boundary
- workspace/product shell is empty but ready for repository attachment

Narration:

"We start with the product. Repositories are implementation evidence, not the
canonical architecture boundary."

### 2:00-4:00 - Attach Four Repositories

Action:

- use **Discover and add** for `customer-api`
- use **Discover and add** for `origination-service`
- use **Discover and add** for `pricing-service`
- use **Discover and add** for `shared-domain-model`

Expected state:

- four repositories are visible under the product
- each repository has source path, build tool, and ownership metadata if known

Narration:

"This product is implemented by four repositories. The tool should reason across
all of them together."

### 4:00-7:00 - Discover Architecture

Action:

- inspect the four retained discovery runs attached by **Discover and add**
- compose Product evidence, then compose cross-repository dependencies

Expected discoveries:

- Maven or Gradle modules
- Spring Boot deployables
- controllers, services, repositories
- package structures
- shared library dependencies
- API specs or controllers
- event and command classes
- pipeline files
- Dockerfiles or deployment descriptors where present

Narration:

"Discovery creates evidence first. Product composition and deterministic analysis
then produce observations, findings, and recommendation options. Proposed
architecture changes remain a later governed action."

### 7:00-9:00 - Define Product Modules And Review Bounded Context Candidates

Explicit Product Modules used for the demo:

- Customer Intake
- Origination
- Pricing
- Shared Model

Bounded-context candidates discussed from the evidence and later available as
explicit governed proposals:

- Customer Context
- Loan Application Context
- Pricing Context
- Shared Domain Model risk candidate

Narration:

"The repository structure is only one signal. The product model combines
repositories, modules, packages, APIs, events, commands, and language."

### 9:00-11:00 - Detect Contracts

Expected contracts:

- API contract between `customer-api` and consumers
- internal API contract between origination and pricing
- event contract such as `ApplicationSubmitted`
- command contract such as `CalculatePrice`

Expected findings:

- contract is present but unversioned
- generated client or shared DTO dependency creates coupling

Narration:

"Contracts are the product architecture joints. If they are unversioned or
hidden in shared DTOs, release independence suffers."

### 11:00-13:00 - Detect Release Dependencies

Expected release dependency evidence:

- shared library version used by multiple services
- pipeline ordering between services
- synchronized artifact versions
- deployment checklist or config linking services

Expected finding:

- `customer-api`, `origination-service`, and `pricing-service` show signs of
  lockstep release due to shared domain model dependency.

Narration:

"This is where repository-by-repository analysis fails. The product view shows
whether multiple repos are actually independent."

### 13:00-15:00 - Detect Distributed Monolith Smells

Expected smells:

- shared library becoming shared domain model
- release lockstep
- unversioned contracts
- cyclic or high repository coupling
- excessive synchronous communication

Expected UI state:

- smell catalogue entries appear as findings
- each finding has evidence and confidence
- recommendations are prioritized

Narration:

"The goal is not to shame the architecture. The goal is to make coupling
visible and actionable."

### 15:00-16:00 - Calculate Architecture Score

Expected scorecard:

- Product modularity
- Repository independence
- Release independence
- Deployment independence
- Contract maturity
- Bounded context cohesion
- Coupling
- Communication complexity
- Ownership clarity
- Operational complexity
- Architecture drift
- Distributed monolith risk
- Overall Product Architecture Score

Narration:

"The score is explainable. It shows confidence and evidence, not just a number."

### 16:00-17:30 - Generate Recommendations

Expected recommendations:

- replace shared domain model dependency with versioned contracts
- define Pricing Context as an explicit bounded context
- add API/event contract versioning
- break repository cycle or release lockstep
- document product module ownership

Narration:

"Recommendations become proposed changes. They still do not mutate the product
graph automatically."

### 17:30-19:00 - Open Review Board And Accept One Recommendation

Action:

- submit recommendation: "replace shared domain model dependency with versioned
  contracts"
- the Workbench opens a persisted Review Board session with the recommendation,
  findings, evidence, analysis, repository, discovery-run, Product, and workspace
  references
- record architect and DDD reviewer votes
- close session
- inspect the explicit `Approved` recommendation state
- choose a concrete graph element type and name, then create the proposed change
- explicitly accept one proposed change

Expected state:

- Review Board decision recommends acceptance
- proposal creation remains a separate explicit action
- accepted proposed change updates product graph
- graph mutation is explicit and audited

Narration:

"Governance remains in the loop. The Review Board recommends; explicit
acceptance mutates the graph."

API sequence used by the UI:

```text
POST .../recommendations/{id}/submit-review
POST .../recommendations/{id}/review/votes
POST .../recommendations/{id}/review/close
POST .../recommendations/{id}/create-proposed-change
POST .../recommendations/{id}/proposed-change/accept
```

The UI shows Candidate, Submitted, Under Review, Approved or Rejected,
Proposed Change, and Architecture Change Accepted as distinct states. Review
history and recommendation lifecycle history remain available after each
transition. Scorecard generation may occur before or after review and does not
cast a vote or determine the Review Board result.

### 19:00-20:00 - View Updated Product Architecture

Expected updated product architecture:

- Product contains four repositories
- product modules and bounded context candidates are visible
- a contract or boundary improvement is accepted into the graph
- distributed monolith risk remains visible with next actions

Closing message:

"Release 0.3 is the shift from repository analysis to product architecture. The
Workbench now asks whether the product is becoming more modular, more
independent, and more governable, or drifting into a distributed monolith."

## Demo Success Criteria

The demo succeeds if viewers understand:

- Product is the primary architectural boundary.
- Repositories are implementation artifacts.
- Multi-repo discovery must reason across code, contracts, releases, ownership,
  and deployment.
- Distributed monolith risk can be detected from evidence.
- Recommendations are governed before graph mutation.
- Product architecture can support both brownfield discovery and greenfield
  design.

## Implemented Release 0.3 Boundaries

The demo uses explicit Product Modules; Release 0.3 does not infer semantic
bounded contexts from names. The final governed proposal can add a bounded
context candidate to the canonical graph only after Review Board approval and
explicit proposal acceptance. Multi-repository discovery is composed from the
retained discovery run attached to each Product repository. Remote repository
providers, runtime telemetry, live AI reviewers, and automatic remediation are
outside Release 0.3.
