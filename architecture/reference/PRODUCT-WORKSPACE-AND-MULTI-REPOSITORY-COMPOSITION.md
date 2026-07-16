# Product Workspace and Multi-Repository Composition

## Boundary model

A Workspace is the local user/project boundary and may contain multiple Products. A Product belongs to exactly one Workspace and is the Release 0.3 architecture subject above Repository. A Repository remains an independently discovered implementation source; its Release 0.2 evidence is authoritative and is never copied into mutable Product domain state.

## Membership and modules

Product membership records a stable repository id, source identity and path/URI, role, lifecycle status, discovery-run ids, optional explicit release/version metadata, and optional ownership metadata. It stores references rather than discovery output. A user may define Product Modules and assign zero or more repositories to them. Modules are never inferred in v0.3.1, and neither repository nor module is treated as a bounded context.

## Composition

`ProductCompositionService` reads referenced discovery-run projections and creates a read-only `ProductCompositionView`. Each composed item retains workspace, product, repository, run, evidence id, plugin, confidence, observed/inferred classification, and source provenance. Repository observations and metrics remain identifiable as repository contributions. Missing runs produce diagnostics and do not erase valid contributions.

Composition does not write into the canonical architecture graph and does not reinterpret evidence as findings.

## Identity resolution

Resolution is conservative. Exact values are grouped only when they occur in multiple repositories. Supported technical keys include explicit contract/event/command identity plus version, channel/queue/topic names, Maven coordinates, OpenAPI operation ids, and package namespaces. Exact matches have confidence 1.0. Similar simple names with different qualified identities or versions are retained as `IdentityConflict` records at reduced confidence; no ambiguous merge occurs.

Relationships describe shared exact identities between repository contributions. They are neutral structural views, not coupling or ownership judgements.

## Metrics

Composition exposes repository, module, run, evidence, relationship, shared contract/channel, unresolved identity, ownership coverage, explicit-version coverage, and repository-to-module assignment coverage. There is no Product Architecture Score.

## Persistence and integrity

Products are workspace-scoped under:

```text
data/workspaces/{workspaceId}/products/{productId}/
  product.json
  repositories.json
  modules.json
  composition.json
```

All nested JSON files participate in the existing workspace SHA-256 manifest. File writes are atomic and the repository reconstructs Product and composition views after restart.

## API and navigation

Thin workspace-scoped endpoints create/list products, manage repository membership and modules, compose evidence, and retrieve composition, relationships, and metrics. The Product UI links discovery-run contributions back to the Release 0.2 evidence explorer; that explorer remains independently available.

## Deliberate limits

v0.3.1 does not infer bounded contexts or modules, classify distributed-monolith or router/ESB smells, calculate architecture scores, create recommendations, or generate proposed changes. Those require later Product-level interpretation over this inspectable composition foundation.
