# Discovery Pipeline

The Discovery Engine uses a staged, pluggable pipeline. The pipeline supports
partial success: a failed plugin should produce a failed plugin result and
diagnostic evidence, but it should not automatically invalidate the full
discovery run.

Canonical flow:

```text
Discovery Request
-> Source Registration
-> Source Validation
-> Repository Discovery
-> Build System Discovery
-> Source Structure Discovery
-> Language Discovery
-> Framework Discovery
-> Dependency Discovery
-> Contract Discovery
-> Messaging Discovery
-> Deployment Descriptor Discovery
-> Evidence Normalisation
-> Deterministic Observation
-> Confidence Calculation
-> AIM Publication
```

## Stage Definitions

| Stage | Purpose | Inputs | Outputs | Evidence Produced | Dependencies | Failure Behaviour | Mode | Required |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Discovery Request | Capture intent, source references, scope, actor, workspace/product context, and correlation id. | User/API request, workspace id, source locations. | Discovery run plan. | Request metadata evidence. | None. | Fail run if required request data is invalid. | Deterministic. | Required. |
| Source Registration | Register repositories, directories, archives, or future runtime/cloud sources. | Discovery run plan. | Registered source list. | Source registration evidence. | Discovery Request. | Skip invalid optional sources; fail if no usable source remains. | Deterministic. | Required. |
| Source Validation | Verify source accessibility, type, permissions, and basic integrity. | Registered sources. | Validated sources and validation diagnostics. | Source validation evidence. | Source Registration. | Mark source failed; continue with other valid sources. | Deterministic. | Required. |
| Repository Discovery | Identify repository boundary, files, directories, docs, ownership hints, and VCS metadata when available. | Validated source. | Repository inventory. | Repository, file, directory, docs, ownership evidence. | Source Validation. | Mark repository plugin failed; continue with lower-level file evidence if available. | Deterministic. | Required for repository sources. |
| Build System Discovery | Detect Maven, Gradle, npm, or other build systems and modules. | Repository inventory. | Build system model and module list. | Build module and dependency declaration evidence. | Repository Discovery. | Continue without build model; downstream plugins receive reduced context. | Deterministic. | Optional but expected. |
| Source Structure Discovery | Identify source roots, test roots, package directories, generated code, resource directories. | Repository inventory, build model. | Source structure model. | Directory, file, package, test evidence. | Repository Discovery; Build System helpful. | Continue with repository file inventory. | Deterministic. | Required for code sources. |
| Language Discovery | Parse language-specific constructs such as Java packages, classes, interfaces, annotations, imports. | Source structure. | Language structure model. | Package, class, interface, annotation evidence. | Source Structure Discovery. | Continue with non-language evidence; mark language evidence incomplete. | Deterministic. | Optional per source. |
| Framework Discovery | Detect frameworks such as Spring through annotations, config, conventions, and dependencies. | Language model, dependencies. | Framework component model. | Controller, service, repository component, configuration evidence. | Language Discovery; Dependency Discovery helpful. | Continue without framework model. | Deterministic. | Optional. |
| Dependency Discovery | Identify module, package, class, artifact, and repository dependencies. | Build model, language model. | Dependency graph. | Dependency declaration and package relationship evidence. | Build System Discovery and Language Discovery. | Continue with partial dependency graph. | Deterministic with heuristic enrichment. | Optional. |
| Contract Discovery | Detect API, event, command, and schema contracts. | Repository files, language model, framework model. | Contract inventory. | API endpoint, OpenAPI, event contract, command contract evidence. | Framework and Language Discovery helpful. | Continue without contract evidence. | Deterministic. | Optional. |
| Messaging Discovery | Detect producers, consumers, topics, queues, brokers, event buses, retry/dead-letter config. | Language model, framework model, config files. | Messaging model. | Message producer, consumer, queue/topic/event bus evidence. | Framework, Contract, Source Structure Discovery. | Continue without messaging model. | Deterministic with heuristics. | Optional. |
| Deployment Descriptor Discovery | Detect deployables, Dockerfiles, Kubernetes, Helm, Terraform, serverless descriptors, pipeline deploy config. | Repository files, build model. | Deployment descriptor model. | Deployable, container, infrastructure descriptor evidence. | Repository Discovery. | Continue without deployment evidence. | Optional deterministic. | Optional. |
| Evidence Normalisation | Convert plugin-specific output into canonical evidence records with provenance and identity. | Plugin outputs. | Normalized evidence set. | Normalized evidence. | All producing plugins. | Drop invalid evidence with diagnostics; preserve valid evidence. | Deterministic. | Required. |
| Deterministic Observation | Produce narrow observations directly supported by evidence. | Normalized evidence. | Discovery observations. | Observation provenance links. | Evidence Normalisation. | Continue with evidence-only output if observation rules fail. | Deterministic. | Required. |
| Confidence Calculation | Assign confidence to evidence and observations based on observation type and corroboration. | Evidence and observations. | Confidence values and rationale. | Confidence rationale evidence. | Evidence Normalisation and Deterministic Observation. | Missing confidence defaults to low/unknown, not high. | Deterministic rules; probabilistic only for AI-assisted plugins. | Required. |
| AIM Publication | Publish evidence and observations into AIM traceability flow. | Evidence, observations, confidence, diagnostics. | AIM Evidence and Observation objects. | AIM publication record. | Confidence Calculation. | Publish partial successful outputs; fail only if AIM boundary unavailable. | Deterministic. | Required. |

## Orchestration

The orchestrator builds a plugin execution plan from source type, requested
scope, plugin capabilities, and plugin dependencies. It should:

- execute required source validation first
- run independent plugins in parallel where safe
- preserve plugin result status
- retain partial evidence from successful plugins
- prevent failed optional plugins from cancelling the run
- attach causation and correlation metadata to outputs
- publish diagnostics as evidence where useful

## Partial Success

A discovery run may complete with status `PARTIAL_SUCCESS` when:

- at least one source is valid
- at least one evidence-producing plugin succeeds
- optional plugins fail or are skipped

Partial success must be visible. It should reduce confidence and explain which
plugins failed, which evidence is missing, and which downstream observations are
therefore unavailable.

## Failure Policy

Fail the complete run only when:

- the request is invalid
- no source can be registered or validated
- evidence normalization cannot preserve any valid evidence
- AIM publication is required and unavailable

Otherwise, record plugin failures and continue.
