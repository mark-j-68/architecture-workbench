# ADR-028 - Local Developer Experience as a First-Class Concern

## Status

Accepted

## Context

Architecture Workbench now spans multiple Java modules, a Spring Boot API shell,
and a React/Vite UI shell. The platform is still intentionally in-memory and
does not yet include persistence, authentication, live AI providers, or event
sourcing.

Without a clear local runbook and smoke test, contributors cannot reliably
verify the Architecture OS workflow or demo the platform without rediscovering
commands, ports, and setup order.

## Decision

Treat local developer experience as a first-class architecture concern.

Add:

- root `README.md` as the local entry point
- `architecture/reference/LOCAL-DEVELOPMENT-RUNBOOK.md`
- `architecture/reference/DEMO-SCRIPT-M5.md`
- `scripts/smoke-test.sh`
- `GET /api/health`

The smoke test verifies the local toolchain, runs the full Maven test suite,
installs frontend dependencies, builds the frontend, and checks API health when
the backend is already running.

The API health endpoint is deliberately simple and does not introduce business
logic or persistence.

## Consequences

Developers can now verify the whole local platform with one command:

```bash
./scripts/smoke-test.sh
```

The expected backend and frontend ports are documented:

- API: `http://localhost:8080`
- UI: `http://127.0.0.1:5173/`

The demo flow is repeatable and aligned with the current kernel workflow:

workspace -> discovery -> findings -> recommendations -> proposed changes ->
Review Board -> explicit acceptance -> graph projection.

## Boundaries

This decision does not add:

- persistence
- authentication or authorization
- live AI providers
- event sourcing
- new architecture business logic

Future platform milestones must keep the local runbook and smoke test current as
new operational dependencies are introduced.
