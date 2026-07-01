# ADR-020 - Architecture Kernel and Event Model

## Status

Accepted

## Context

Architecture Workbench has moved from an M2 modelling workbench toward an
AI-native Architecture Operating System. The platform now has emerging modules
for the architecture knowledge graph, architecture intelligence, discovery,
review-board coordination, workspace boundaries, MCP boundaries, and decision
intelligence.

Continuing feature development without a clear kernel, meta-model, and event
model would risk creating disconnected services that use similar words but
different lifecycle rules, ownership boundaries, traceability semantics, and
audit expectations.

## Decision

Pause runtime feature expansion and define the Architecture Kernel Design Pack
before adding more runtime behavior.

The design pack establishes:

- the Architecture OS reference architecture
- the canonical Architecture Kernel meta-model
- the immutable architecture event model

The graph remains the canonical runtime boundary for architecture structure.
The Architecture Kernel owns the rules, event semantics, lifecycle semantics,
traceability invariants, and audit relevance expectations that govern graph,
intelligence, review, discovery, generation, provider, and MCP behavior.

## Consequences

Future API, UI, MCP, provider, persistence, discovery, healthcheck, generation,
and review-board work must align with the kernel contracts before adding new
runtime behavior.

The platform gains a common vocabulary for workspace scope, graph mutation,
evidence-backed intelligence, decision learning, projections, provider
invocations, MCP tool calls, and audit events.

This ADR deliberately does not introduce new Java modules, live providers,
persistence, REST APIs, or UI implementation.

## References

- `architecture/reference/ARCHITECTURE-OS-REFERENCE-ARCHITECTURE.md`
- `architecture/reference/ARCHITECTURE-KERNEL-META-MODEL.md`
- `architecture/reference/ARCHITECTURE-EVENT-MODEL.md`
