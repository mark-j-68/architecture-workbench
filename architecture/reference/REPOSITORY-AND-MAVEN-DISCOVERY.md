# Repository And Maven Discovery

v0.2.1 implements the first deterministic Discovery Engine plugins:

- Repository Discovery Plugin
- Maven Discovery Plugin

These plugins start the Release 0.2 evidence-first model. They observe source
structure and Maven build metadata. They do not create architectural findings,
recommendations, decisions, or graph mutations.

## Repository Discovery Plugin

Plugin id:

```text
repository.discovery
```

Category:

```text
Source Plugin
```

The plugin detects:

- repository root
- directory structure
- README files
- docs directories
- ADR directories
- source directories
- test directories
- build files
- Docker files
- obvious CI/CD files

Outputs:

- `DiscoveryEvidence`
- narrow `DiscoveryObservation`
- `DiscoveryConfidence`
- provenance references to local paths

Confidence:

- direct file and directory existence: `1.0`
- repository root: inferred from markers such as `.git`, `pom.xml`, `README.md`,
  and `src`

## Maven Discovery Plugin

Plugin id:

```text
maven.discovery
```

Category:

```text
Build Plugin
```

The plugin detects:

- `pom.xml`
- `groupId`
- `artifactId`
- `version`
- `packaging`
- parent coordinates
- modules
- dependencies
- build plugins where straightforward
- multi-module structure

Outputs:

- Maven build-file evidence
- build-module evidence
- parent evidence
- module declaration evidence
- dependency declaration evidence
- build plugin evidence
- deterministic Maven observations

Confidence:

- `pom.xml` file existence: observed fact
- parsed Maven coordinates: high confidence
- module, dependency, and plugin declarations: high confidence when parsed from
  `pom.xml`

## Compatibility

`LocalRepositoryDiscoveryConnector` remains compatible with the existing
Discovery API and tests. It delegates repository and Maven detection to the new
plugins, adapts plugin evidence back into legacy `DiscoveredArtifact` values,
and keeps the existing Java/Spring/configuration scan in place.

This preserves the current API behavior while introducing the new plugin
boundary.

## AIM Mapping

`DiscoveryPluginAimMapper` maps:

```text
DiscoveryEvidence -> AIM Evidence
DiscoveryObservation -> AIM Observation
```

This preserves:

```text
Discovered Source -> Evidence -> Observation -> Finding -> Recommendation
```

Findings and recommendations are still produced by the intelligence layer.

## Boundaries

This milestone does not add:

- JavaParser
- Spring analysis plugins
- multi-repo product reasoning
- AI providers
- REST API expansion
- UI changes
- persistence changes
- event sourcing

The Discovery Engine observes. The Architecture Intelligence Engine
interprets. The Review Board governs. The Knowledge Graph records accepted
state.
