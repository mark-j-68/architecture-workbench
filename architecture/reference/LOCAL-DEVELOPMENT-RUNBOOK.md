# Local Development Runbook

This runbook explains how to run, verify, and troubleshoot Architecture
Workbench locally.

## Prerequisites

Install:

- Java 21
- Maven
- Node.js
- npm

Check versions:

```bash
java -version
mvn -version
node --version
npm --version
```

## Expected Ports

| Service | URL |
| --- | --- |
| Architecture API | `http://localhost:8080` |
| API health | `http://localhost:8080/api/health` |
| Vite UI | `http://127.0.0.1:5173/` |

## Full Verification

From the repository root:

```bash
./scripts/smoke-test.sh
```

The script:

- verifies Java and Maven are available
- verifies Node and npm are available
- runs full Maven tests
- installs frontend dependencies
- runs the frontend production build
- checks `/api/health` if the backend is already running

## Backend Tests

From the repository root:

```bash
mvn test
```

This runs all Java module tests, including the Spring Boot API integration test.

## Frontend Build

From `workbench-ui`:

```bash
npm ci
npm run build
```

Use `npm install` instead of `npm ci` only when intentionally updating
dependencies.

## Running The Backend

From the repository root, first install local reactor artifacts:

```bash
mvn install -DskipTests
```

Then start the API:

```bash
mvn -pl architecture-api org.springframework.boot:spring-boot-maven-plugin:3.2.3:run \
  -Dspring-boot.run.mainClass=com.architectureworkbench.api.ArchitectureApiApplication
```

Verify:

```bash
curl http://localhost:8080/api/health
```

Expected response shape:

```json
{
  "status": "UP",
  "service": "architecture-api",
  "timestamp": "..."
}
```

## Running The Frontend

In a second terminal:

```bash
cd workbench-ui
npm ci
npm run dev -- --host 127.0.0.1
```

Open:

```text
http://127.0.0.1:5173/
```

The Vite dev server proxies `/api` to `http://localhost:8080`.

If the API is running elsewhere:

```bash
VITE_API_BASE_URL=http://localhost:8080 npm run dev -- --host 127.0.0.1
```

## Local Demo Data

Discovery needs a repository path that the backend process can read. For a
quick demo, use this repository root:

```text
/data/home/mark/Documents/Personal/Research/architecture-workbench-m2
```

Any local Maven or Spring project can also be used.

## Troubleshooting

### Backend Cannot Resolve Local Modules

Run:

```bash
mvn install -DskipTests
```

Then start `architecture-api` again.

### Maven Cannot Resolve `spring-boot:run`

Use the full plugin coordinate:

```bash
mvn -pl architecture-api org.springframework.boot:spring-boot-maven-plugin:3.2.3:run \
  -Dspring-boot.run.mainClass=com.architectureworkbench.api.ArchitectureApiApplication
```

### Port 8080 Is Already In Use

Stop the existing process or run the API on another port:

```bash
mvn -pl architecture-api org.springframework.boot:spring-boot-maven-plugin:3.2.3:run \
  -Dspring-boot.run.mainClass=com.architectureworkbench.api.ArchitectureApiApplication \
  -Dspring-boot.run.arguments=--server.port=8081
```

If using port `8081`, start the UI with:

```bash
VITE_API_BASE_URL=http://localhost:8081 npm run dev -- --host 127.0.0.1
```

### Frontend Cannot Find `tsc` Or `vite`

Run:

```bash
cd workbench-ui
npm ci
```

### Frontend Cannot Reach API

Check backend health:

```bash
curl http://localhost:8080/api/health
```

If the backend is not on port `8080`, set `VITE_API_BASE_URL`.

### Discovery Returns No Useful Results

Use a local project with some of:

- `pom.xml`
- Java package directories
- Spring controllers or services
- README
- `architecture/adr`
- test directories
- Dockerfile

Discovery is intentionally shallow at this milestone and does not perform deep
static analysis.

## Development Boundaries

Local development currently uses in-memory state. Restarting the backend loses
workspaces, discovery runs, review sessions, and proposed changes.

Do not add business rules to the API or UI shells. Kernel semantics belong in
the kernel and service modules.
