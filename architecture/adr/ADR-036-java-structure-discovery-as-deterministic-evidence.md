# ADR-036 - Java Structure Discovery as Deterministic Evidence

## Status

Accepted

## Context

ADR-034 establishes a pluggable, evidence-first Discovery Engine. ADR-035
implements repository and Maven discovery as its first deterministic plugins.
The next discovery stage needs Java language structure and package dependency
relationships so that later intelligence can reason from source evidence.

Java discovery can easily cross the boundary from observation into semantic or
architectural judgement. Full symbol solving would also add complexity before
the required evidence model and failure behaviour have been established.

## Decision

Implement two v0.2.2 deterministic plugins in the `discovery-engine` module:

- `JavaStructureDiscoveryPlugin`, id `java.structure`
- `PackageDependencyDiscoveryPlugin`, id `package.dependency`

Java structure discovery uses repository scanning and lightweight,
comment/string-aware parsing. It records conventional Java source and test
roots, packages, type declarations, annotations, imports, and straightforward
declared inheritance or implementation relationships.

Package dependency discovery consumes Java structure evidence. It records
package-to-package relationships from imports and cross-module package
references when the observed source packages belong to different Maven module
paths. When an external import prefix straightforwardly matches a Maven
dependency `groupId` declared by the importing module, it records that
correlation at reduced confidence.

Every produced evidence item preserves the plugin id, repository-relative file
path, relevant package and class, source line when available, stable identity,
and explicit confidence rationale.

The plugins emit only evidence and narrow deterministic observations such as:

```text
Package com.example.app imports package com.example.domain.
Class MortgageService implements MortgagePort.
Module app references package com.example.domain in module domain.
```

They do not emit architecture findings or recommendations.

`LocalRepositoryDiscoveryConnector` adapts Java package evidence back to the
legacy `JAVA_PACKAGE` artifact to preserve existing API behaviour. Dependency
relationships remain typed plugin evidence because the legacy artifact model
has no correct equivalent.

## Consequences

Java source facts and package dependencies are repeatable, traceable, and
available to the Architecture Intelligence Model.

Syntactically incomplete files can still provide safe partial evidence because
discovery does not require a successful compilation unit or type resolution.

Stable evidence ids allow unchanged facts to retain identity across discovery
runs.

Import-to-Maven correlation remains deliberately conservative. A `groupId`
prefix match is supporting evidence, not resolved class ownership.

Future Spring, contract, messaging, modularity, bounded-context, and
distributed-monolith analysis can consume this evidence without being embedded
in the source scanner.

## Boundaries

This decision does not add:

- JavaParser or another semantic parser
- symbol or type resolution
- Spring-specific analysis
- bounded-context inference
- layering or modularity findings
- distributed-monolith analysis
- AI providers
- REST API or UI changes
- persistence changes
- direct graph mutation

Discovery observes. Architecture Intelligence interprets. Review Board
governs. Knowledge Graph records accepted state.
