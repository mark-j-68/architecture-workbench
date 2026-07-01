# ADR-007 — Immutable Activity Log with PII Separation

## Status
Accepted

## Context
Architecture Workbench is intended for regulated environments. The platform needs a durable record of who did what, which model or plugin acted, what was decided, and which artefacts changed. At the same time, the immutable log must not become a permanent store of personal data.

## Decision
Use an immutable activity log based on non-PII audit envelopes. The envelope records operational facts such as activity id, timestamp, actor reference, workspace id, action, model/provider id, decision outcome, correlation id, previous hash, payload hash, and envelope hash.

Sensitive content and PII must be excluded from the immutable envelope. Where a sensitive payload is required, the envelope stores only an encrypted payload reference and content hash. The encrypted payload is stored separately with its own key reference and classification metadata.

## Consequences
- The audit trail remains tamper-evident and append-only.
- PII is not written directly into immutable records.
- Sensitive payloads can be made unreadable without destroying the audit trail.
- Query/reporting components must understand the distinction between envelope metadata and protected payloads.

## Implementation Notes
The initial local implementation may use filesystem append-only JSONL for development. The target AWS implementation should support S3 Object Lock or another immutable append-only store, with a hash chain linking envelopes.
