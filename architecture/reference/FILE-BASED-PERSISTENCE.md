# File-Based Persistence

M6.1 adds local JSON persistence so the demoable M5 workflow survives backend
restart without introducing a database, JPA, Spring Data, files as a domain
model, or event sourcing.

## Storage Location

Default:

```text
./data/workspaces
```

Override order:

1. Spring/system property: `architecture.workbench.storage.dir`
2. Environment variable: `ARCHITECTURE_WORKBENCH_STORAGE_DIR`
3. Default path: `./data/workspaces`

The API uses file-backed adapters by default. Set:

```text
architecture.workbench.persistence=in-memory
```

to use in-memory adapters for tests or throwaway local runs.

## Layout

The intended local layout is:

```text
data/
  workspaces/
    {workspaceId}/
      manifest.json
      workspace.json
      graph.json
      proposed-changes.json
      review-board-sessions.json
      audit-events.json
```

Some existing kernel events and proposed changes still carry the graph id as
their workspace boundary because earlier M4/M5 workflows used graph id as the
runtime identifier. Those files may appear under a graph-id directory until the
workspace id and graph id contract is normalized.

## Persisted Data

File-backed adapters now persist:

- workspace metadata through `FileWorkspaceRepository`
- graph snapshots through `FileArchitectureGraphRepository`
- proposed changes through `FileProposedChangeRepository`
- review-board session response snapshots through the API adapter store
- immutable audit/event records through `FileAuditSink`

Graph import/export compatibility remains based on the existing graph snapshot
contract. JSON persistence is an adapter representation, not a replacement for
the canonical `ArchitectureKnowledgeGraph`.

## Safety Behaviour

Missing files are treated as empty state.

Corrupt JSON fails fast with an `IllegalStateException` that includes the file
path and the operation that failed. The adapter does not silently discard
corrupt files.

Audit records retain the existing hash-chain behaviour. Restarting the
`FileAuditSink` reloads prior records and appends subsequent records using the
previous hash.

## Integrity And Recovery

M6.2 adds a workspace `manifest.json` with SHA-256 checksums and the last known
audit hash. `FileWorkspaceIntegrityService` verifies persisted files and audit
hash-chain continuity.

JSON writes now create `{fileName}.bak`, write new content through a temporary
file, and move the new content into place atomically where practical.

If a current JSON file is corrupt but its backup is valid, reads recover from
the backup and refresh the manifest. If both are corrupt, the read fails with a
clear error.

See [PERSISTENCE-INTEGRITY-AND-RECOVERY.md](PERSISTENCE-INTEGRITY-AND-RECOVERY.md).

## Boundaries

This milestone deliberately does not add:

- database persistence
- event sourcing
- JPA or Spring Data
- file locking for concurrent API instances
- schema migrations
- authentication or authorization
- live AI provider calls

Local JSON storage is suitable for single-user demos, smoke tests, and early
workflow validation. It is not the long-term multi-user persistence strategy.
