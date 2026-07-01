# Demo Script M5

This script demonstrates the M5 local platform: Spring Boot API, React/Vite UI,
local discovery, Review Board workflow, explicit proposed-change acceptance, and
graph projection viewing.

## Setup

Terminal 1:

```bash
mvn install -DskipTests
mvn -pl architecture-api org.springframework.boot:spring-boot-maven-plugin:3.2.3:run \
  -Dspring-boot.run.mainClass=com.architectureworkbench.api.ArchitectureApiApplication
```

Verify:

```bash
curl http://localhost:8080/api/health
```

Terminal 2:

```bash
cd workbench-ui
npm ci
npm run dev -- --host 127.0.0.1
```

Open:

```text
http://127.0.0.1:5173/
```

## Demo Narrative

Architecture Workbench treats architecture as a governed knowledge graph. The UI
is not the architecture model; it is a workflow adapter over the API. Discovery,
recommendations, proposed changes, Review Board decisions, and graph projections
are all routed through the kernel boundary.

## Walkthrough

### 1. Create Workspace

In the Workspaces panel:

1. Enter a workspace name.
2. Click Create.

Expected result:

- workspace appears in the sidebar
- workspace detail shows its graph id
- graph viewer is initially empty

### 2. Run Local Discovery

In Run Discovery:

1. Enter a readable local repository path.
2. Click Run Local Discovery.

Example path:

```text
/data/home/mark/Documents/Personal/Research/architecture-workbench-m2
```

Expected result:

- discovery metrics populate
- findings appear
- recommendation candidates appear
- proposed changes appear

Message to explain:

Discovery produces evidence-backed intelligence. It does not mutate the graph
directly.

### 3. Inspect Recommendations And Proposed Changes

Select one recommendation and one proposed change.

Expected result:

- selected rows are highlighted
- proposed changes remain in `PROPOSED` status

Message to explain:

Intelligence outputs become proposed changes first. Proposed changes carry
traceability to recommendations, findings, evidence, workspace, and correlation
id.

### 4. Open Review Board Session

Click Open Session.

Expected result:

- Review Board session opens with human architect and DDD reviewer participants

Message to explain:

The Review Board is a governed decision workflow. It recommends action but does
not mutate the graph.

### 5. Record Votes

Click:

1. Architect Approve
2. DDD Approve
3. Close

Expected result:

- Review Board decision is `ACCEPT_PROPOSED_CHANGE`
- graph is still unchanged

Message to explain:

Review closure does not apply graph changes. Acceptance remains explicit.

### 6. Accept Proposed Change

In Proposed Changes, click Accept on the reviewed proposed change.

Expected result:

- proposed change status becomes `ACCEPTED`
- graph refresh shows a new architecture element

Message to explain:

Only accepted proposed changes mutate the graph, and mutation is routed through
validated graph services.

### 7. Generate Projection

Click Generate Projection.

Expected result:

- graph projection viewer renders the current graph through React Flow

Message to explain:

The graph viewer is a projection consumer, not a graph editor.

## Optional API Checks

List workspaces:

```bash
curl http://localhost:8080/api/workspaces
```

Check health:

```bash
curl http://localhost:8080/api/health
```

## Demo Boundaries

This milestone intentionally does not include:

- database persistence
- authentication or authorization
- live AI provider calls
- event sourcing
- direct graph editing from the UI

Restarting the backend clears in-memory state.
