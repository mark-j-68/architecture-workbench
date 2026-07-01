# Minimal UI Workflow

The Minimal Web UI Shell is a React/Vite adapter over the Architecture API. It
does not define architecture business rules and does not mutate the kernel
directly.

## Purpose

The UI provides a thin operational surface for the Architecture OS kernel
workflow:

1. create or select a workspace
2. run local repository discovery
3. view findings
4. view recommendation candidates
5. view proposed architecture changes
6. open a Review Board session
7. record reviewer votes
8. close the Review Board session
9. explicitly accept, reject, or defer proposed changes
10. view the updated Architecture Knowledge Graph projection

## Adapter Boundaries

The UI calls the Architecture API through `architectureApiClient`.

React components do not call kernel modules and do not implement graph mutation,
Review Board decision rules, DDD validation, discovery interpretation, or
proposal lifecycle rules.

API DTOs are kept in `architectureApiTypes`. UI graph view models are mapped in
`workflowViewModels`, so API response shapes do not leak directly into the React
Flow viewer.

## Screens

### Workspace List And Create Workspace

The sidebar lists workspaces returned from `GET /api/workspaces` and creates
new workspaces through `POST /api/workspaces`.

### Workspace Detail

The selected workspace detail displays workspace name and graph id. It can
refresh the current graph and generate a React Flow projection.

### Run Local Discovery

The discovery panel accepts a local repository path and calls
`POST /api/workspaces/{workspaceId}/discovery/local`.

The UI assumes the backend can access the supplied path. It does not scan the
repository itself.

### Discovery Results

Findings, recommendations, and proposed changes are loaded from the API after a
discovery run. The UI only displays and selects these objects.

### Review Board Session

The UI opens a Review Board session for selected recommendation and proposed
change ids, then records votes and closes the session through API endpoints.

The UI does not derive the Review Board decision locally.

### Proposed Change Actions

Accept, reject, and defer actions call the proposed-change API endpoints.

Only acceptance can mutate the graph, and that mutation occurs server-side
through `ProposedChangeService` and validated graph services.

### Architecture Graph Projection Viewer

The graph viewer uses React Flow. It can render the current graph response or a
generated `REACT_FLOW` projection.

The viewer is a projection consumer only. It is not a graph editor.

## Development Runtime

During local development, Vite proxies `/api` to `http://localhost:8080`.

For alternate API locations, set `VITE_API_BASE_URL`.

## Non-Goals

The Minimal Web UI Shell does not add:

- persistence
- authentication or authorization
- live AI providers
- event sourcing
- direct graph editing
- client-side Review Board decision rules
- client-side discovery scanning
