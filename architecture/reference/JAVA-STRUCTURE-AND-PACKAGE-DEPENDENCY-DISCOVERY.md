# Java Structure And Package Dependency Discovery

v0.2.2 adds two deterministic Discovery Engine plugins:

- `JavaStructureDiscoveryPlugin`
- `PackageDependencyDiscoveryPlugin`

They extend repository and Maven discovery with language structure and import
relationships. Their outputs are evidence and narrow observations only. They do
not create architectural findings, recommendations, proposed changes, or graph
mutations.

## Java Structure Discovery Plugin

Plugin id:

```text
java.structure
```

Category:

```text
Language Plugin
```

Required dependency:

```text
repository.discovery
```

Optional dependency:

```text
maven.discovery
```

The plugin scans Java files in stable path order. It masks comments and string
or character literals before applying lightweight declaration parsing. This
keeps discovery deterministic and prevents source-like text in comments and
literals from becoming evidence.

It detects:

- conventional `src/main/java` source roots
- conventional `src/test/java` test roots
- package declarations
- classes
- interfaces
- enums
- records
- annotation declarations
- annotation uses
- imports, including static and wildcard imports
- straightforward `extends` relationships
- straightforward `implements` relationships

When a Java file belongs to a Maven module, the nearest ancestor containing a
`pom.xml` supplies the stable module path. No Maven model building or type
resolution is required.

### Java Evidence Types

| Evidence type | Meaning | Directness | Default confidence |
| --- | --- | --- | --- |
| `java-source-root` | A main Java source-root directory exists. | Observed | `1.0` |
| `java-test-source-root` | A test Java source-root directory exists. | Observed | `1.0` |
| `java-package` | A Java package declaration exists. | Observed | `1.0` |
| `java-class` | A class declaration exists. | Observed | `1.0` |
| `java-interface` | An interface declaration exists. | Observed | `1.0` |
| `java-enum` | An enum declaration exists. | Observed | `1.0` |
| `java-record` | A record declaration exists. | Observed | `1.0` |
| `java-annotation-declaration` | An annotation type declaration exists. | Observed | `1.0` |
| `java-annotation` | An annotation use exists. | Observed | `1.0` |
| `java-import` | An import declaration exists. | Observed | `1.0` |
| `java-inheritance` | A type declares an `extends` relationship. | Observed | `0.9` |
| `java-implementation` | A type declares an `implements` relationship. | Observed | `0.9` |

The relationship confidence is slightly lower than declaration confidence
because the lightweight parser records the declared target text without
resolving its symbol.

## Package Dependency Discovery Plugin

Plugin id:

```text
package.dependency
```

Category:

```text
Dependency Plugin
```

Required dependency:

```text
java.structure
```

Optional dependency:

```text
maven.discovery
```

The plugin consumes normalized Java import, Java package, and Maven dependency
evidence. It does not rescan source files.

For every import with a declared source package, it applies these deterministic
rules:

1. Match the import against observed repository packages using the longest
   package prefix.
2. Emit an internal or external `package-dependency` relationship.
3. If an internal target belongs to another Maven module, emit a
   `module-package-reference` relationship.
4. If an external import prefix matches the `groupId` of a dependency declared
   by the importing Maven module, emit an `external-dependency-reference`.

Matching an import to a Maven dependency by `groupId` is intentionally limited.
It is emitted only as a deterministic correlation with `0.85` confidence, not
as proof that a particular class came from the artifact.

### Dependency Evidence Types

| Evidence type | Observation | Confidence |
| --- | --- | --- |
| `package-dependency` | Package A imports package B. | `0.9` |
| `module-package-reference` | Module A imports a package observed in module B. | `0.9` |
| `external-dependency-reference` | An import prefix matches a dependency `groupId` declared by the importing Maven module. | `0.85` |

Self-package imports are not emitted as package dependencies.

## Provenance And Identity

Every v0.2.2 evidence item contains:

- a stable name-based evidence id
- the producing plugin id in `source` and `attributes.pluginId`
- repository-relative `filePath`
- line number when a declaration or import has a source location
- package and class names when relevant
- Maven module path when one is available
- a confidence value and rationale
- supporting evidence ids for derived dependency evidence

Stable ids are derived from evidence type, semantic identity, path, and line.
Repeated scans of unchanged files therefore retain evidence identities even
though each discovery run has its own timestamp.

## AIM Mapping

`DiscoveryPluginAimMapper` maps the new outputs without special cases:

```text
DiscoveryEvidence -> AIM Evidence
DiscoveryObservation -> AIM Observation
```

Package and module observations retain links to the dependency evidence that
directly supports them. Imports and Maven declarations are also recorded as
supporting evidence ids on the discovery output.

## Failure And Partial Source Behaviour

The plugins preserve partial success:

- a project with no Java source succeeds with empty Java evidence
- incomplete Java files still contribute declarations and imports that can be
  recognized safely
- an unreadable individual Java file becomes a diagnostic while other files
  continue
- a missing Java prior output produces a package-plugin diagnostic and no
  dependency evidence
- repository traversal failure fails only the affected plugin

## Local Connector Compatibility

`LocalRepositoryDiscoveryConnector` now delegates Java package discovery to
`java.structure` and adapts `java-package` evidence to the existing
`JAVA_PACKAGE` legacy artifact. Spring-specific compatibility scanning is
delegated to the dedicated v0.2.3 framework plugins described in
`SPRING-APPLICATION-DISCOVERY.md`.

Package dependency evidence has no equivalent legacy `DiscoveredArtifactType`.
It remains available through the plugin evidence API and AIM mapping rather
than being flattened into an unrelated legacy artifact.

## Explicit Boundaries

v0.2.2 does not implement:

- Java symbol solving or full semantic static analysis
- generic type or method-call dependency resolution
- transitive Maven dependency resolution
- Spring-specific discovery
- bounded-context inference
- layering judgements
- distributed-monolith analysis
- findings such as God service or poor layering
- AI-assisted interpretation
- direct knowledge-graph mutation

These facts remain inputs to later Architecture Intelligence stages.
