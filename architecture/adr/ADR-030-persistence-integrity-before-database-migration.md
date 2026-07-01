# ADR-030 - Persistence Integrity Before Database Migration

## Status

Accepted

## Context

M6.1 introduced file-based JSON persistence so the local Architecture Workbench
demo survives backend restart. That persistence is intentionally lightweight,
but local files can still be corrupted by interrupted writes, manual edits, or
partial filesystem failures.

The platform is not ready for database persistence or event sourcing. The
workspace, graph, review, proposal, and audit contracts should remain stable
before those infrastructure choices are made.

## Decision

Add integrity and recovery support to file-based persistence before introducing
database persistence.

Each workspace now has a `manifest.json` containing:

- workspace id
- schema version
- created and updated timestamps
- SHA-256 checksums for persisted JSON files
- last known audit hash

Add `FileWorkspaceIntegrityService` to verify checksums and audit hash-chain
continuity.

Add backup-on-write for JSON files. Before overwriting a JSON file, copy the
current file to `{fileName}.bak`, write the new content to a temporary file, and
move it into place atomically where practical.

Add recovery-on-read. If the current JSON file is corrupt but its backup is
valid, recover the current file from the backup. If both are corrupt, fail
safely with a clear error.

## Consequences

Local persistence is safer for demos and development:

- accidental corruption can be detected
- interrupted writes have a simple recovery path
- audit hash-chain breaks are visible
- the last known audit hash is recorded in the workspace manifest

This improves trust in local file storage without changing the kernel semantics.

## Boundaries

This decision does not add:

- a database
- event sourcing
- REST API expansion
- UI changes
- live AI providers
- authentication or authorization
- multi-process file locking
- schema migration tooling

JSON remains an adapter format. It must not become the canonical domain model,
and all state changes must continue to go through validated application
services.
