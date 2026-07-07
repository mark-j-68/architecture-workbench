# Discovery Evidence Taxonomy

Discovery plugins normalize their outputs into canonical evidence types. Each
evidence item must include source, provenance, identity, confidence, references,
supporting artifacts, and whether it is directly observed or inferred.

| Evidence Type | Source | Provenance | Identity Strategy | Confidence Characteristics | Possible Relationships | Observed Or Inferred |
| --- | --- | --- | --- | --- | --- | --- |
| Repository | source registration, VCS metadata, local path | source URI/path, scan timestamp, plugin id | normalized repository URL or absolute path hash | high when source exists and is readable | contains files, directories, build modules, deployables | directly observed |
| File | filesystem or repository inventory | repository id, path, checksum, timestamp | repository id + normalized path + checksum | high when file readable and checksum captured | belongs to directory, supports contracts/classes/docs | directly observed |
| Directory | filesystem or repository inventory | repository id, path, scan timestamp | repository id + normalized path | high when directory readable | contains files, packages, source roots, test roots | directly observed |
| Build Module | Maven/Gradle build files | build file path, parser version | build system + group/artifact/module path | high when declared in build file | belongs to repository, declares dependencies, may create deployable | directly observed |
| Dependency Declaration | pom, Gradle, lock files, manifests | file path, declaration location | declaring module + dependency coordinates | high for explicit declarations; lower for inferred transitive dependencies | links modules, libraries, repositories, contracts | directly observed or inferred |
| Package | Java/Kotlin/other source files | source path, package declaration | language + package name + source root | high when declared in source | contains classes/interfaces, depends on packages | directly observed |
| Class | source parser | file path, class declaration location | fully qualified class name + repository/module | high when parsed from source | belongs to package, has annotations, implements interfaces | directly observed |
| Interface | source parser | file path, interface declaration location | fully qualified interface name + repository/module | high when parsed from source | implemented by classes, defines contracts or ports | directly observed |
| Annotation | source parser | file path, annotated element, annotation name | annotated element id + annotation type | high when parsed; medium when inferred from generated metadata | marks controllers/services/configuration/events | directly observed |
| Controller | framework parser | class evidence plus framework annotation | class id + controller annotation | high for explicit annotations; medium for naming conventions | exposes API endpoints, belongs to deployable | deterministic inference |
| Service | framework parser | class evidence plus annotation/name | class id + service marker | high for explicit annotations; lower for naming-only | belongs to layer/context, depends on repositories/contracts | deterministic or heuristic inference |
| Repository Component | framework parser | class evidence plus repository annotation/interface | class id + repository marker | high for explicit annotations or Spring Data interfaces | accesses data, may indicate persistence layer | deterministic inference |
| Configuration Component | framework/config parser | class or file evidence | config element id | high for explicit config files/classes | configures deployables, messaging, dependencies | directly observed or deterministic inference |
| API Endpoint | framework parser, OpenAPI | controller method or spec operation | method route + HTTP verb or OpenAPI operation id | high when route explicitly declared; higher when controller and spec agree | exposed by deployable, realizes API contract | deterministic inference |
| OpenAPI Contract | OpenAPI file | spec path, version, checksum | contract name/version/path | high when valid spec parsed | describes endpoints, providers, consumers | directly observed |
| Event Contract | schema, class, AsyncAPI, messaging config | schema path/class/topic references | event name + version + producer context | high for explicit schemas; medium for event classes | produced/consumed by contexts and deployables | directly observed or deterministic inference |
| Command Contract | command class, API operation, schema | class/spec path and handler evidence | command name + version + provider | high with explicit class and handler; lower with naming only | consumed by handlers, initiates behavior | deterministic or heuristic inference |
| Message Producer | code/config parser | producer call, annotation, binding config | producer element + topic/queue | medium to high depending on explicitness | produces event/command to queue/topic | deterministic inference |
| Message Consumer | code/config parser | listener annotation, binding config, handler | consumer element + topic/queue | medium to high depending on explicitness | consumes event/command from queue/topic | deterministic inference |
| Queue/Topic/Event Bus Reference | config, code, infrastructure descriptor | broker config path, topic name, binding | broker/ref name + environment if known | high when explicit in config; lower from string constants | connects producers and consumers | directly observed or deterministic inference |
| Deployable Unit | build plugin, Dockerfile, app bootstrap, deployment descriptor | build file, Dockerfile, manifest, main class | deployable name + module/repository | medium to high depending on corroboration | contains components, exposes contracts, deployed to environments | deterministic inference |
| Container Descriptor | Dockerfile, Compose, Kubernetes, Helm | file path, checksum | image/build context + path | high when descriptor parsed | packages deployable, maps ports/config | directly observed |
| Infrastructure Descriptor | Terraform, Kubernetes, Helm, serverless files | file path, resource type, checksum | resource type + name + path | high when parsed; lower for unsupported syntax | defines environments, deployables, queues, databases | directly observed |
| Test Evidence | test directories, test classes, contract tests | path, framework markers, class names | test path/class + module | high for explicit test files | supports modules, contracts, behavior, confidence | directly observed |
| ADR/Documentation Evidence | markdown/docs/ADR paths | file path, title, checksum | doc path + title | high for files; lower for semantic interpretation | supports decisions, policies, context, ownership | directly observed |
| Ownership Evidence | CODEOWNERS, metadata, service catalogue, docs | file/source path, team ref | owned element + team ref | high for explicit CODEOWNERS; medium for docs | assigns ownership to repos/modules/contracts | directly observed or heuristic inference |
| Release/Version Evidence | pipelines, tags, manifests, build versions | pipeline file, version file, artifact coords | release stream + artifact/version | high for explicit version and pipeline files | links repositories, deployables, contracts, release streams | directly observed |

## Identity Requirements

Evidence identity must be stable across repeated scans when the underlying
source has not materially changed. File-sensitive evidence should include path
and checksum. Semantic evidence should include source identity and semantic
name.

## Relationship Requirements

Evidence relationships must be explicit and typed. Discovery may emit evidence
relationships such as:

- file belongs to repository
- class belongs to package
- controller exposes endpoint
- deployable contains component
- producer publishes to topic
- consumer subscribes to topic
- module declares dependency
- contract describes endpoint

These relationships are evidence relationships. They are not automatically
accepted architecture graph relationships.
