# North Star Demo 0.3

Release 0.3 demonstrates Multi-Repo Product Architecture Intelligence.

It consumes Release 0.2 repository discovery outputs and reasons across them at
Product level.

The demo length is 20 minutes.

## Demo Promise

"Architecture Workbench now understands a product, not just a repository. We
can attach several repositories, discover how the product is actually built,
infer modules and bounded contexts, detect contracts and release dependencies,
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

- attach `customer-api`
- attach `origination-service`
- attach `pricing-service`
- attach `shared-domain-model`

Expected state:

- four repositories are visible under the product
- each repository has source path, build tool, and ownership metadata if known

Narration:

"This product is implemented by four repositories. The tool should reason across
all of them together."

### 4:00-7:00 - Discover Architecture

Action:

- run product discovery

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

"Discovery creates evidence first, then observations, findings, recommendations,
and proposed architecture changes."

### 7:00-9:00 - Infer Product Modules And Bounded Contexts

Expected inferred product modules:

- Customer Intake
- Origination
- Pricing
- Shared Model

Expected bounded context candidates:

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

- open Review Board for recommendation: "replace shared domain model dependency
  with versioned contracts"
- record architect and DDD reviewer votes
- close session
- explicitly accept one proposed change

Expected state:

- Review Board decision recommends acceptance
- accepted proposed change updates product graph
- graph mutation is explicit and audited

Narration:

"Governance remains in the loop. The Review Board recommends; explicit
acceptance mutates the graph."

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
