# Discovery Plugin Catalogue 0.2

This catalogue defines the initial Release 0.2 plugin roadmap. It is a design
catalogue only; it does not implement plugins.

## Repository Discovery Plugin

Inputs: registered local repository source.

Evidence: repository, file, directory, README/docs, ADR/documentation evidence,
ownership hints.

Deterministic observations: repository is readable; source tree contains files;
documentation and ADR paths are present or absent.

Dependencies: Source Registration and Source Validation.

Implementation milestone: v0.2 baseline.

## Maven Discovery Plugin

Inputs: repository file inventory, `pom.xml` files.

Evidence: build modules, Maven coordinates, dependency declarations, packaging,
parent/child module relationships.

Deterministic observations: Maven project detected; multi-module build
detected; module depends on declared artifact.

Dependencies: Repository Discovery Plugin.

Implementation milestone: v0.2 baseline.

## Java Structure Discovery Plugin

Inputs: source directories, Java files, build module model.

Evidence: packages, classes, interfaces, annotations, imports, test evidence.

Deterministic observations: class belongs to package; package exists in module;
test source root exists; class imports another package.

Dependencies: Repository Discovery Plugin; Maven Discovery Plugin optional.

Implementation milestone: v0.2.2.

## Spring Discovery Plugins

Inputs: Java structure evidence, dependencies, resource/config files.

Evidence: application entry points, configuration and profiles, controllers,
services, repository components, API endpoints, transactions, data entities,
dependency injection, and Spring messaging integration.

Deterministic observations: annotated class is Spring controller/service/repo;
route mapping exposes endpoint; configuration class exists.

Dependencies: Repository Discovery Plugin; Java Structure Discovery Plugin;
Maven Discovery Plugin optional.

Implementation milestone: v0.2.3.

## Package Dependency Plugin

Inputs: Java packages, classes, imports, build modules.

Evidence: package dependency relationships, module dependency relationships,
dependency declarations.

Deterministic observations: package A imports package B; module A declares
dependency on module/library B.

Dependencies: Java Structure Discovery Plugin; Maven Discovery Plugin helpful.

Implementation milestone: v0.2.2.

## OpenAPI Discovery Plugin

Inputs: repository file inventory, OpenAPI files, Spring endpoint evidence.

Evidence: OpenAPI contracts, API endpoints, operation ids, path/version
metadata.

Deterministic observations: OpenAPI contract exists; endpoint declared in spec;
controller endpoint has matching or unmatched spec candidate.

Dependencies: Repository Discovery Plugin; Spring Discovery Plugin optional.

Implementation milestone: v0.2.4.

## Messaging Discovery Plugin

Inputs: Java structure evidence, Spring config, messaging dependencies,
infrastructure descriptors.

Evidence: message producers, message consumers, queue/topic/event bus
references, event contracts, command contracts.

Deterministic observations: listener consumes topic/queue; producer publishes
to topic/queue; event or command class exists.

Dependencies: Java Structure Discovery Plugin; Spring Discovery Plugin helpful.

Implementation milestone: v0.2.4 for event, command, version, ownership, and
topology evidence; v0.2.3 for Spring messaging markers.

## Contract Version and Ownership Plugins

Inputs: API, event, command, schema, repository, Maven, package, CODEOWNERS,
and documentation evidence.

Evidence: explicit versions, version headers and paths, compatibility and
deprecation declarations, ownership contacts, CODEOWNERS matches, Maven/package
namespace indicators.

Deterministic observations: contract declares version; contract contains no
explicit version field; source declares or indicates an owner.

Dependencies: Contract Discovery Plugins; Repository and Maven Discovery
Plugins helpful.

Implementation milestone: v0.2.4.

## Deterministic Structural Analysis Plugins

Release 0.2.5 adds seven deterministic analysis plugins:

- `analysis.package-cycle`
- `analysis.module-dependency`
- `analysis.layer-structure`
- `analysis.component-dependency`
- `analysis.contract-version`
- `analysis.messaging-topology`
- `analysis.dependency-metrics`

They derive cycles, direction, candidate technical layers, connectivity, literal version comparisons, bounded topology paths, and traceable metrics from prior evidence. They emit observations only and do not create findings, recommendations, scorecards, proposed changes, or Product-level interpretations. See `DETERMINISTIC-STRUCTURAL-ANALYSIS.md` and ADR-039.

## Deployment Descriptor Plugin

Inputs: repository file inventory, build modules, Docker/Kubernetes/Helm/serverless
files.

Evidence: deployable units, container descriptors, infrastructure descriptors,
environment references.

Deterministic observations: Dockerfile exists; Kubernetes deployment references
container image; serverless handler exists.

Dependencies: Repository Discovery Plugin; Maven Discovery Plugin helpful.

Implementation milestone: v0.2 enhanced discovery.

## Documentation/ADR Discovery Plugin

Inputs: repository file inventory and documentation directories.

Evidence: ADR/documentation evidence, decision records, README files,
architecture docs, ownership evidence.

Deterministic observations: ADR directory exists; ADR files exist; README
exists; docs mention architecture sections.

Dependencies: Repository Discovery Plugin.

Implementation milestone: v0.2 baseline.

## Out Of Catalogue For v0.2

The following are future plugin categories and should not be implemented as
Release 0.2 baseline requirements:

- live cloud runtime discovery
- tracing/metrics discovery
- GitHub PR discovery
- AI-assisted semantic interpretation
- portfolio-level product analysis
- distributed monolith scoring

These future plugins must still follow the same evidence-first contract.
