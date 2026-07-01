# Architecture Workbench

Architecture Workbench is an AI-native architecture platform. Its core is a
governed Architecture Knowledge Graph, with discovery results, reviews,
recommendations, proposed changes, ADRs, diagrams, OpenAPI, and generated views
treated as projections or workflows over the same kernel.

The current local stack is intentionally minimal:

- Spring Boot API shell over the Architecture OS kernel
- React/Vite web UI shell
- file-backed local workspace storage by default
- no database persistence
- no live AI providers
- no authentication or authorization yet
- no event sourcing

## Repository Layout

- `architecture-api`: thin Spring Boot API adapter over the kernel
- `workbench-ui`: minimal React/Vite workflow UI
- `architecture-knowledge-graph`: canonical graph domain and projection services
- `architecture-intelligence`: evidence, observations, findings, recommendations, and decisions
- `discovery-engine`: local repository discovery and healthcheck foundation
- `review-board`: governed Review Board workflow
- `workspace-service`: workspace, graph, proposed-change, and local JSON persistence adapters
- `platform-audit`: typed architecture events and immutable audit sink
- `architecture/reference`: reference architecture, runbooks, workflow docs, and gap analyses
- `architecture/adr`: architecture decision records

## Prerequisites

- Java 21
- Maven
- Node.js
- npm

## Verify Everything

From the repository root:

```bash
./scripts/smoke-test.sh
```

The smoke test checks Java, Maven, Node, and npm, then runs:

```bash
mvn test
cd workbench-ui && npm ci && npm run build
```

If the backend is already running on `http://localhost:8080`, it also checks:

```bash
curl http://localhost:8080/api/health
```

## Run Backend

From the repository root:

```bash
mvn install -DskipTests
mvn -pl architecture-api org.springframework.boot:spring-boot-maven-plugin:3.2.3:run \
  -Dspring-boot.run.mainClass=com.architectureworkbench.api.ArchitectureApiApplication
```

Expected backend port:

```text
http://localhost:8080
```

Health endpoint:

```bash
curl http://localhost:8080/api/health
```

By default, local workspace state is stored under:

```text
./data/workspaces
```

Override it with either:

```bash
ARCHITECTURE_WORKBENCH_STORAGE_DIR=/path/to/workspaces
```

or:

```bash
mvn -pl architecture-api org.springframework.boot:spring-boot-maven-plugin:3.2.3:run \
  -Dspring-boot.run.mainClass=com.architectureworkbench.api.ArchitectureApiApplication \
  -Dspring-boot.run.arguments=--architecture.workbench.storage.dir=/path/to/workspaces
```

Use in-memory adapters explicitly with:

```bash
--architecture.workbench.persistence=in-memory
```

## Run Frontend

In a second terminal:

```bash
cd workbench-ui
npm ci
npm run dev -- --host 127.0.0.1
```

Expected frontend URL:

```text
http://127.0.0.1:5173/
```

The Vite dev server proxies `/api` to `http://localhost:8080`.

## Demo Flow

1. Start the backend.
2. Start the frontend.
3. Open `http://127.0.0.1:5173/`.
4. Create a workspace.
5. Enter a local repository path.
6. Run discovery.
7. Review findings, recommendations, and proposed changes.
8. Open a Review Board session.
9. Record reviewer votes.
10. Close the session.
11. Accept a proposed change.
12. Refresh or generate the graph projection.

See [DEMO-SCRIPT-M5.md](architecture/reference/DEMO-SCRIPT-M5.md) for a fuller
walkthrough.

## Troubleshooting

- If the frontend cannot reach the API, confirm the backend is running on
  `localhost:8080` and check `/api/health`.
- If `spring-boot:run` cannot resolve the plugin prefix, use the full plugin
  coordinate shown above.
- If the API cannot resolve local modules, run `mvn install -DskipTests` first.
- If `npm run build` cannot find `tsc` or `vite`, run `npm ci` in
  `workbench-ui`.
- If port `8080` or `5173` is already in use, stop the conflicting process or
  run the service on another port.
- If local state looks stale, inspect or remove `./data/workspaces`.
- If local JSON state appears corrupt, see the manifest and backup behaviour in
  the persistence integrity guide.

## More Detail

- [LOCAL-DEVELOPMENT-RUNBOOK.md](architecture/reference/LOCAL-DEVELOPMENT-RUNBOOK.md)
- [FILE-BASED-PERSISTENCE.md](architecture/reference/FILE-BASED-PERSISTENCE.md)
- [PERSISTENCE-INTEGRITY-AND-RECOVERY.md](architecture/reference/PERSISTENCE-INTEGRITY-AND-RECOVERY.md)
- [MINIMAL-UI-WORKFLOW.md](architecture/reference/MINIMAL-UI-WORKFLOW.md)
- [ARCHITECTURE-INTELLIGENCE-WORKFLOW.md](architecture/reference/ARCHITECTURE-INTELLIGENCE-WORKFLOW.md)
- [PLATFORM-CONSTITUTION.md](PLATFORM-CONSTITUTION.md)
