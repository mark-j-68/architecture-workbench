# ADR-035 - Repository and Maven Discovery as First Deterministic Plugins

## Status

Accepted

## Context

ADR-034 defines the Discovery Engine as pluggable and evidence-first. Release
0.2 needs concrete deterministic plugins to establish the implementation shape
without turning discovery into a monolithic scanner or moving architectural
judgement into scanner code.

Repository structure and Maven build metadata are the lowest-risk starting
point because they are directly observable and provide useful evidence for later
language, framework, dependency, contract, and product-level reasoning.

## Decision

Implement the first deterministic Discovery Engine plugins inside the existing
`discovery-engine` module:

- `RepositoryDiscoveryPlugin`
- `MavenDiscoveryPlugin`

Also introduce the plugin model types:

- `DiscoveryPlugin`
- `DiscoveryPluginId`
- `DiscoveryPluginMetadata`
- `DiscoveryPluginCapability`
- `DiscoveryPluginDependency`
- `DiscoveryInput`
- `DiscoveryOutput`
- `DiscoveryEvidence`
- `DiscoveryObservation`
- `DiscoveryConfidence`
- `DiscoveryExecutionContext`
- `DiscoveryPluginResult`
- `DiscoveryPluginStatus`

The plugins produce evidence and narrow deterministic observations only.

`LocalRepositoryDiscoveryConnector` delegates repository and Maven detection to
the plugins while preserving existing artifact output and compatibility.

## Consequences

The Discovery Engine now has a concrete plugin boundary.

Repository and Maven discovery can evolve independently from later Java,
Spring, dependency, OpenAPI, messaging, deployment, runtime, cloud, and
AI-assisted plugins.

The current API and workflow remain compatible because existing
`DiscoveredArtifact` output is still produced.

Discovery evidence can be mapped into AIM Evidence and Discovery observations
can be mapped into AIM Observations without creating findings or
recommendations in plugin code.

## Boundaries

This decision does not add:

- JavaParser
- Spring analysis plugin
- multi-repo product reasoning
- live AI providers
- REST API expansion
- UI changes
- persistence changes
- event sourcing

Discovery continues to observe. Architecture Intelligence interprets. Review
Board governs. Knowledge Graph records accepted architectural state.
