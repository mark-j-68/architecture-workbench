# M2 Governance Addendum — AI Consensus, Immutable Audit, and PII Protection

## Purpose
This addendum extends M2 before implementation. It introduces regulatory-grade governance into the canonical Architecture Model rather than adding it later as an afterthought.

## New M2 Scope
M2 now includes:

- LLM Consensus / Judge Engine model.
- Immutable activity log envelope model.
- PII separation policy.
- Cryptographic shredding policy.
- Validation rules for governance configuration.
- ADR-006, ADR-007, and ADR-008.

## LLM Consensus Flow

```text
Candidate proposal
  ↓
Claude judge assessment
OpenAI judge assessment
  ↓
Consensus coordinator
  ↓
Accepted / rejected / needs revision / human review
  ↓
Immutable audit activity
```

The consensus engine should be used for material AI-assisted decisions, not every low-risk text transformation.

## Audit and PII Flow

```text
Activity occurs
  ↓
Non-PII immutable envelope written
  ↓
Sensitive payload encrypted separately if required
  ↓
Envelope stores protected payload reference + hash
  ↓
Erasure request destroys relevant key material
  ↓
Audit trail remains; PII payload is unreadable
```

## Design Principle
The immutable log proves that something happened. The encrypted payload stores sensitive content only while the relevant key exists.
