# ADR-008 — Cryptographic Shredding for GDPR Erasure

## Status
Accepted

## Context
Some Architecture Workbench inputs may contain personal data, for example event-storming boards, mortgage-domain examples, logs, prompts, uploaded images, or AI responses. Regulated users may need to retain an audit trail while also supporting erasure workflows for protected personal data.

## Decision
PII-bearing payloads must be encrypted separately from immutable audit envelopes. Encryption should use a key hierarchy that allows practical erasure by destroying a data encryption key, wrapped key, or subject/case-specific key reference. This is the platform's cryptographic shredding mechanism.

The immutable envelope remains as a non-PII record of activity. The encrypted payload becomes unreadable once the relevant key material is destroyed.

## Consequences
- Erasure workflows become key-management workflows, not physical deletion of every immutable record.
- The platform must classify payloads and prevent accidental PII in envelopes.
- Key references, destruction requests, approvals, and outcomes must themselves be audited.
- Backups, exports, vector stores, prompts, embeddings, and generated artefacts must be included in the PII protection design.

## Non-Goals
This ADR does not claim that cryptographic shredding automatically satisfies every legal erasure obligation in every scenario. It establishes the technical architecture required to support erasure-compatible regulated operation.
