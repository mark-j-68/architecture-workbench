# ADR-027 - UI as Projection and Workflow Adapter

## Status

Accepted

## Context

The Architecture API exposes a thin HTTP shell over the Architecture OS kernel.
The platform now needs a minimal web UI so users can exercise the kernel
workflow without introducing another source of business rules.

The main risk is allowing React components to become a second architecture
engine by deriving decisions, mutating graph state, interpreting discovery
results, or bypassing proposed-change governance.

## Decision

Implement the Minimal Web UI Shell as a React/Vite adapter over the
Architecture API.

The UI provides screens for:

- workspace list and workspace creation
- workspace detail
- local discovery execution
- discovery findings
- recommendation candidates
- proposed changes
- Review Board sessions and votes
- Architecture Knowledge Graph projection viewing

The UI uses an API client abstraction and keeps API DTOs separate from graph UI
view models. React Flow is used for graph projection viewing because it is
already available in the project.

The UI does not contain business rules. All significant operations are delegated
to the API:

- discovery runs
- Review Board session opening
- vote recording
- Review Board closure
- proposal acceptance, rejection, and deferral
- projection generation
- graph retrieval

## Consequences

The web app can exercise the basic Architecture OS workflow:

create workspace -> run discovery -> view findings and recommendations -> review
proposed change -> vote -> close Review Board session -> explicitly accept a
proposed change -> view updated graph projection.

Graph mutation remains server-side and still passes through
`ProposedChangeService` and validated graph application services.

The UI can evolve independently from the kernel because HTTP DTOs and UI view
models are separated.

## Boundaries

The UI is a projection and workflow adapter only.

It must not:

- directly mutate the Architecture Knowledge Graph
- derive Review Board decisions locally
- interpret discovery artifacts into findings locally
- call AI providers directly
- bypass the Architecture API
- duplicate kernel validation rules

Persistence, authentication, authorization, live AI providers, event sourcing,
and advanced graph editing remain deferred.
