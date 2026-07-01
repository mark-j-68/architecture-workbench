# Persistence Integrity and Recovery

M6.2 strengthens local JSON persistence for demo and development use. It keeps
the M6.1 boundary intact: JSON files are adapters, not the domain model, and no
database or event sourcing is introduced.

## Manifest

Each workspace directory has a `manifest.json` file.

The manifest records:

- workspace id
- schema version
- created timestamp
- updated timestamp
- SHA-256 checksums for persisted JSON files
- last known audit hash

Checked files:

```text
workspace.json
graph.json
proposed-changes.json
review-board-sessions.json
audit-events.json
```

Only files that exist are included in the manifest. Missing files with no
manifest checksum are treated as absent optional state.

## Integrity Verification

`FileWorkspaceIntegrityService` verifies:

- manifest presence
- manifest workspace id
- manifest schema version
- workspace metadata checksum
- graph checksum
- proposed changes checksum
- review-board sessions checksum
- audit/event file checksum
- audit hash-chain continuity
- manifest last-known audit hash

A verification result is returned as `WorkspaceIntegrityReport`. It includes a
boolean validity flag, failure messages, and the last known audit hash.

## Backup-on-Write

Before overwriting a JSON file, the current file is copied to:

```text
{fileName}.bak
```

New JSON is written to a temporary sibling file and then moved into place. The
move uses an atomic move where the filesystem supports it and falls back to a
replace move otherwise.

After a successful write, the workspace manifest is refreshed.

## Recovery

When reading JSON:

- if the current file is valid, it is used
- if the current file is corrupt and `.bak` is valid, the backup is copied back
  over the current file and the manifest is refreshed
- if both current and backup files are corrupt, the read fails with a clear
  `IllegalStateException`

This gives local/demo runs a simple recovery path for interrupted writes or
manual file damage without silently hiding unrecoverable corruption.

## Boundaries

This mechanism is deliberately modest.

It does not provide:

- database durability
- multi-process locking
- event sourcing replay
- schema migration tooling
- distributed consistency
- security or access control

Database persistence remains a later adapter decision. The kernel workflow must
continue to mutate state only through validated application services.
