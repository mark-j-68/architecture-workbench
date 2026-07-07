# North Star Demo Script

This is the concrete 15-minute Release 0.2 demo narrative for Architecture
Workbench: Discovery Foundations.

## Opening Message

"Architecture Workbench is an AI-native architecture platform. For Release 0.2,
we are focusing on Discovery Foundations: take an existing Spring Boot system,
collect evidence from code, create deterministic structural observations, pass
those observations into the Architecture Intelligence Model, govern proposed
changes through a Review Board, and update the architecture graph only when a
change is explicitly accepted."

## Setup Commands

From the repository root:

```bash
mvn install -DskipTests
```

Start the backend:

```bash
mvn -pl architecture-api org.springframework.boot:spring-boot-maven-plugin:3.2.3:run \
  -Dspring-boot.run.mainClass=com.architectureworkbench.api.ArchitectureApiApplication
```

In a second terminal, start the frontend:

```bash
cd workbench-ui
npm ci
npm run dev -- --host 127.0.0.1
```

Open:

```text
http://127.0.0.1:5173/
```

Optional reset before a clean demo:

```bash
rm -rf data/workspaces
```

Use a local Spring Boot project path. For a self-demo, this repository can be
used as a stand-in Maven project, but the strongest demo uses a compact sample
Spring Boot codebase with controllers, services, repositories, tests, and ADRs.

## User Journey

### 1. Create Workspace

Narration:

"I start by creating a workspace. A workspace represents one architecture
initiative: a product, system, modernization effort, or imported codebase."

Action:

- enter workspace name: `Mortgage Origination Discovery`
- create workspace

Expected UI state:

- workspace appears in the workspace list
- workspace detail opens
- graph projection is empty or minimal
- discovery action is available

### 2. Run Local Discovery

Narration:

"Now I point the tool at an existing Spring Boot repository. Discovery is local
and static in this release. It does not call a live AI provider."

Action:

- paste local project path
- run discovery

Expected UI state:

- discovery run completes
- findings count is greater than zero
- recommendations count is greater than zero
- proposed changes count is greater than zero

Expected discovered signals:

- Maven `pom.xml` files
- Maven modules
- Java packages
- Spring controllers
- Spring services
- Spring repositories
- configuration files
- README or docs
- test directories

### 3. Inspect Evidence, Observations, Findings, Recommendations

Narration:

"The important point is that discovery does not jump straight to opinion. It
creates evidence first, then deterministic observations. Findings and
recommendations belong to the Architecture Intelligence Model."

Expected UI state:

- findings list is populated
- recommendation candidates are populated
- each recommendation can be traced to findings and evidence

Expected findings and recommendations:

- `Multiple modules detected`
  - recommendation: preserve or formalize module boundaries
- `Spring controllers detected`
  - recommendation: represent API layer in the architecture graph
- `Repository classes detected`
  - recommendation: identify persistence adapter boundary
- `Missing ADR directory` or weak ADR signal
  - recommendation: record key architecture decisions
- `No obvious domain layer` or unclear domain signal
  - recommendation: inspect bounded context and aggregate candidates

Expected AIM findings from Release 0.2 evidence:

- package cycle detected
- controller depends directly on repository
- bounded context candidate inferred from module/package cluster
- modular monolith or layered architecture style inferred
- architecture score inputs explain strengths and risks when interpreted by AIM

### 4. View Proposed Changes

Narration:

"Discovery does not mutate the canonical graph. It proposes changes. That is a
governance boundary."

Expected UI state:

- proposed changes list is visible
- each proposed change has a status such as `PROPOSED`
- selected proposed change links to recommendation, findings, evidence, and
  correlation id

Expected proposed changes:

- add component for an API/controller area
- add component for a service/application area
- add component or bounded context candidate for a module/package cluster

### 5. Open Review Board Session

Narration:

"Before changing the graph, I open a Review Board session. The Review Board is
where human and automated reviewers can approve, reject, defer, or ask for more
evidence."

Action:

- select one recommendation
- select one proposed change
- open Review Board session

Expected UI state:

- session opens
- participants are listed
- vote controls are available

### 6. Record Votes

Narration:

"I record two approvals to show the happy path. If reviewers disagree, the
decision can defer or ask for more evidence."

Action:

- Lead Architect votes `APPROVE`
- DDD Reviewer votes `APPROVE`
- close session

Expected UI state:

- votes appear in session
- session closes
- decision is `ACCEPT_PROPOSED_CHANGE`

### 7. Accept Proposed Change

Narration:

"The Review Board recommends acceptance, but it still does not mutate the graph.
The user explicitly accepts the proposed change."

Action:

- click accept on the proposed change

Expected UI state:

- proposed change status becomes `ACCEPTED`
- graph has a new element or relationship
- audit/event record is emitted

### 8. View Updated Graph Projection

Narration:

"The graph projection updates from the canonical knowledge graph. The diagram is
a projection, not the source of truth."

Action:

- open or refresh graph projection

Expected UI state:

- accepted element or relationship is visible
- projection source element refs include the accepted graph change

### 9. Verify Audit And Traceability

Narration:

"Now I can trace why this graph element exists. It came from a proposed change,
from a recommendation, from findings, from observations, from evidence in the
repository."

Expected state:

- workspace persists under `data/workspaces`
- `manifest.json` exists
- audit/event records exist where supported
- accepted proposed change retains recommendation, finding, evidence, and
  correlation ids
- graph mutation was explicit and validated

### 10. Ask "What Should I Do Next?"

Narration:

"The final user question is: what should I do next? Release 0.2 provides the
evidence and observations; the intelligence layer should answer with
prioritized, evidence-backed recommendations rather than a generic report."

Expected answer:

- address high-confidence structural findings first
- resolve missing documentation or ADR traceability
- inspect DDD boundary candidates
- review coupling/cycle risks
- accept, reject, or defer proposed graph changes through governance

## Closing Message

"This is the product direction for Release 0.2. Architecture Workbench observes
an existing system, preserves evidence and confidence, lets the intelligence
layer explain what it means, updates the graph only after explicit acceptance,
and preserves traceability from code evidence to architecture decision."
