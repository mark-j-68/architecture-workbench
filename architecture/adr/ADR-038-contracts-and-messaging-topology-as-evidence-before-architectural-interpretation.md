# ADR-038 - Contracts and Messaging Topology as Evidence Before Architectural Interpretation

## Status

Accepted

## Context

ADR-034 establishes a pluggable evidence-first Discovery Engine, and ADR-037
keeps Spring framework detection separate from architectural judgement. HTTP
contracts, event and command schemas, message producers and consumers, queues,
topics, event buses, routing handlers, versions, compatibility declarations,
and ownership indicators expose a communication topology needed by later
product architecture analysis.

The same topology can represent healthy choreography, deliberate orchestration,
an integration boundary, or problematic central routing. Static declarations
alone do not establish which interpretation is correct. Mixing those
interpretations into contract scanners would make discovery context-dependent
and would prematurely introduce v0.3 Product concepts into the v0.2 model.

## Decision

Implement deterministic v0.2.4 OpenAPI, Event Contract, Command Contract,
Messaging Topology, Contract Version, and Contract Ownership Evidence plugins.

APIs, events, commands, channels, producers, consumers, versions,
compatibility declarations, and ownership indicators are discovery evidence.
Plugins may compose exact declarations into narrow directional topology
observations and must retain repository-relative provenance, confidence,
observed/inferred status, and source evidence ids.

Explicit schemas, annotations, and static configuration are observed facts.
Exact correlations are high-confidence deterministic evidence. Naming and
location conventions are permitted only where unambiguous, are labelled
inferred, and carry reduced confidence. Dynamic expressions remain unresolved
and retain explicit uncertainty.

Invalid or incomplete contract documents produce partial success and structured
parse-error evidence without discarding valid evidence or failing unrelated
plugins. Discovery never executes repository application code and does not call
AWS or other cloud APIs.

Contract discovery does not mutate the canonical knowledge graph. Evidence and
observations are published into AIM. Any inferred graph addition must use the
Proposed Change workflow and requires explicit governance before graph mutation.

Product-level ownership, compatibility judgement, ESB drift, orchestration
smells, bounded-context inference, architecture-style classification, and
distributed-monolith conclusions belong to Release v0.3 Architecture
Intelligence, not these plugins.

## Consequences

The Workbench can build a repeatable, evidence-backed communication inventory
from single repositories without credentials or runtime access.

OpenAPI operations can be conservatively correlated with exact Spring endpoint
evidence. EventBridge, SQS, SNS, Kafka, RabbitMQ, AsyncAPI, schema, and Java
markers can form traceable topology edges while remaining neutral about their
architectural quality.

Ownership conflicts and absent versions remain visible as independent facts or
absence observations. Discovery does not collapse conflicting indicators into
a canonical owner and does not turn missing version data into a risk finding.

Lightweight parsing cannot resolve runtime destinations, remote schema
references, custom framework composition, or semantic message intent. Those
limitations remain explicit rather than being replaced by unsupported guesses.

## Boundaries

This decision does not add:

- distributed-monolith analysis
- ESB or router smell detection
- bounded-context inference
- Product-level reasoning or ownership decisions
- OpenAPI generation
- cloud API or AWS credential use
- live AI providers
- canonical graph mutation
- REST API, UI, or persistence expansion
- event sourcing

Discovery records communication evidence. Architecture Intelligence interprets
it. Proposed Changes and Review Board governance control graph evolution.
