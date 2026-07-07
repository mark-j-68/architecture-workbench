# Release 0.2 North Star Demo

Release 0.2 is the Discovery Foundations release. The demo should show
Architecture Workbench turning an existing Spring Boot codebase into
evidence-backed structural observations that the Architecture Intelligence Model
can interpret into findings, recommendations, proposed changes, and governed
graph change.

The target demo length is 15 minutes.

## Demo Promise

In 15 minutes, an architect can point Architecture Workbench at an existing
Spring Boot project and get:

- discovered architecture evidence
- deterministic observations grounded in that evidence
- findings and recommendations produced by the intelligence layer
- proposed graph changes that do not mutate the model automatically
- a governed Review Board decision
- an accepted architecture graph update
- an audit trail of the workflow
- a clear answer to: "what should I do next?"

## Scenario

The user is an architect joining a product team with a working Spring Boot
service. Documentation is incomplete, the structure is partly implicit, and the
team wants a quick architecture healthcheck before planning modernization work.

The demo uses a local Spring Boot/Maven repository with:

- multiple Maven modules
- controllers, services, and repositories
- package structure that suggests API, application, domain, and persistence
  layers
- at least one DDD or coupling smell
- test and README coverage that can be evaluated

## 15-Minute Flow

### 0:00-1:00 - Create Workspace

The user opens Architecture Workbench and creates a workspace named after the
existing system.

Expected experience:

- workspace appears in the workspace list
- empty graph/projection is available
- the platform treats the workspace as one architecture initiative

### 1:00-3:00 - Run Discovery

The user enters the local path to the Spring Boot project and runs discovery.

Expected experience:

- discovery starts against a local repository source
- Maven modules are identified
- Spring controllers, services, repositories, configuration files, docs, and
  tests are identified
- discovery creates evidence before observations; findings belong to the
  Architecture Intelligence Model

### 3:00-6:00 - Inspect Evidence, Observations, Findings, Recommendations

The user reviews the discovery output.

Expected experience:

- evidence identifies files, packages, modules, and source paths
- deterministic observations summarize narrow structural facts
- findings explain architectural risks or strengths through AIM
- recommendations explain practical next steps through AIM
- every finding traces back to observations and evidence

Example findings:

- multiple Maven modules indicate a modular structure worth preserving
- controllers are present, so an API layer exists
- repository classes are present, so persistence concerns are visible
- missing or weak ADRs reduce decision traceability
- package dependencies suggest bounded context candidates or coupling risks

### 6:00-8:00 - View Proposed Architecture Changes

The user opens proposed changes generated from recommendation candidates.

Expected experience:

- proposed changes are visible as candidates, not automatic graph mutations
- each proposed change links to recommendation, finding, evidence, workspace,
  and correlation id
- candidate changes include element additions such as components, systems,
  capabilities, or bounded context candidates

### 8:00-11:00 - Open Review Board Session And Vote

The user opens a Review Board session for one recommendation and one proposed
change. Participants vote.

Expected experience:

- session includes the selected recommendation and proposed change
- participants can include human architect, DDD reviewer, security reviewer,
  delivery reviewer, or automated reviewer stubs
- votes can approve, reject, defer, request more evidence, or approve with
  conditions
- conflicting votes do not mutate the graph

### 11:00-12:30 - Accept One Proposed Change

The Review Board recommends acceptance. The user explicitly accepts one proposed
change.

Expected experience:

- acceptance remains a separate action from Review Board close
- only accepted changes mutate the architecture graph
- graph mutation goes through validated application services
- typed architecture events and audit records are emitted

### 12:30-13:30 - See Graph/Projection Update

The user refreshes or generates the graph projection.

Expected experience:

- new graph element or relationship is visible
- projection reflects the canonical graph
- UI makes clear that the graph changed because a proposed change was accepted

### 13:30-14:30 - Verify Audit And Traceability

The user inspects traceability and audit evidence.

Expected experience:

- discovery, review, and accepted graph mutation are traceable
- audit records include typed architecture events where supported
- persisted local storage includes manifest, checksums, and audit hash chain
- recommendation, proposed change, and decision can be traced to evidence

### 14:30-15:00 - Ask "What Should I Do Next?"

The user asks what to do next after discovery.

Expected experience:

- platform prioritizes recommendations by architecture impact, confidence, and
  effort
- next action is evidence-backed
- answer distinguishes between immediate cleanup, deeper discovery, review, and
  deferred work

## North Star Outcome

The demo should make one product idea obvious:

Architecture Workbench is not a diagramming tool. Release 0.2 observes existing
code and publishes traceable evidence. The intelligence and governance layers
turn that evidence into controlled graph evolution.
