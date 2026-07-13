# Contract and Messaging Discovery

Release v0.2.4 adds deterministic contract and messaging topology discovery to
the evidence-first Discovery Engine. It inventories declared communication
surfaces and exact static relationships. It does not interpret whether those
relationships are architecturally desirable.

## Boundary

The plugins answer factual questions:

- Which API, event, command, and message contracts are present?
- Which endpoints, schemas, channels, producers, and consumers are declared?
- Which versions, compatibility settings, and ownership indicators are explicit?
- Which communication edges can be composed directly from those declarations?
- What file, module, symbol, line, and prior evidence support each fact?

They do not answer whether a topology is an ESB, an orchestration smell, a
distributed monolith, a bounded-context violation, or an ownership problem.
Those are v0.3 Architecture Intelligence concerns.

## Plugins

### OpenApiContractDiscoveryPlugin

Plugin id: `contract.openapi`.

Supported inputs:

- OpenAPI YAML and JSON
- Swagger YAML and JSON
- prior Spring HTTP endpoint evidence

Evidence includes document and API versions, title, paths, HTTP operations,
operation ids, request and response schema references, component schemas,
security schemes, servers, and deprecated operations. An exact method-and-path
match may link a Spring endpoint evidence id to an OpenAPI operation. No fuzzy
endpoint matching and no OpenAPI generation occur.

### EventContractDiscoveryPlugin

Plugin id: `contract.event`.

Supported inputs:

- JSON Schema
- Avro record schemas
- AsyncAPI definitions
- explicitly annotated Java event types
- Java classes or records with event naming/location conventions
- EventBridge `putEvents` calls and static builder values
- visible EventBridge rule, target, and archive descriptors

Evidence includes event name, version, schema format/path, envelope markers,
compatibility metadata, declared owner, AsyncAPI channels and roles, producers,
bus/topic names, source, and detail type.

### CommandContractDiscoveryPlugin

Plugin id: `contract.command`.

Supported inputs:

- JSON/YAML command schemas
- explicitly annotated Java command types
- Java command naming/location conventions
- SQS envelopes, listeners, clients, and templates

Evidence includes command name, version, queue and reply queue, correlation and
causation markers, envelope status, producers, and consumers. A Java type is not
classified as a command merely because it is used in messaging. Convention-only
classification requires a `Command` name, command source location, or a clear
SQS-envelope convention and is marked inferred.

### MessagingTopologyDiscoveryPlugin

Plugin id: `messaging.topology`.

This plugin composes exact prior evidence into directional topology edges:

```text
Producer -> event bus / topic / queue
Event bus / topic / queue -> Consumer
Event -> Handler
Handler -> Command
Command producer -> SQS queue
SQS queue -> Command consumer
```

It also records visible dead-letter queues, retry queues, EventBridge archives,
and SNS subscription/fan-out configuration. Spring Kafka, RabbitMQ, and SQS
listener destinations are adapted from `spring.messaging` evidence.

Every composed edge lists its source evidence ids. The output is evidence and a
narrow observation, not a canonical graph mutation.

### ContractVersionDiscoveryPlugin

Plugin id: `contract.version`.

The plugin detects explicit contract/schema/API versions, semantic versions,
versioned paths, version headers, compatibility metadata, deprecation, and
supersession declarations. If a discovered contract has neither an explicit
field nor a versioned path, the plugin emits only a factual
`contract-version-absent` observation. It does not emit an unversioned-contract
risk or recommendation.

### ContractOwnershipEvidencePlugin

Plugin id: `contract.ownership`.

Ownership indicators are collected independently from:

- `CODEOWNERS`
- Maven group/artifact namespaces
- Java package namespaces
- OpenAPI/AsyncAPI contact metadata
- schema `owner` or `x-owner` metadata
- documentation owner/team/contact fields
- repository and module location

Explicit owner/contact declarations use high confidence. Pattern matches and
namespace indicators are labelled as inferences with reduced confidence. If
sources disagree, all evidence is retained; discovery does not choose a
canonical owner.

## Typed Discovery Concepts

The implementation introduces small v0.2 value objects and records:

- `ContractId`
- `ContractType`
- `ContractVersion`
- `ContractEndpoint`
- `MessageChannel` and `MessageChannelType`
- `ProducerReference` and `ConsumerReference`
- `ContractOwnershipEvidence`
- `ContractCompatibilityEvidence`

Plugin results remain canonical `DiscoveryEvidence` and
`DiscoveryObservation`, allowing existing AIM publication without persistence,
REST, UI, or graph schema changes. Product, bounded-context, and deployable
concepts are intentionally absent from this model.

## Evidence and Provenance

Contract and topology evidence includes:

- repository-relative `filePath`
- Maven `module` where available
- Java package and class for source evidence
- symbol or configuration location
- line number where available
- stable plugin id
- observed or inferred classification
- numeric confidence and rationale
- source evidence ids for correlations and composed edges

IDs and timestamps are deterministic for the same input and plugin version.
Files are scanned in repository-relative path order. No application code is
loaded or executed, and no cloud credentials or AWS API calls are used.

## Confidence Rules

| Evidence | Default confidence | Classification |
| --- | ---: | --- |
| Explicit schema, annotation, operation, channel, queue, topic, bus, contact, or configuration | 1.0 | observed |
| Exact Spring/OpenAPI method-and-path correlation | 0.9 or higher | deterministic correlation |
| Edge copied from an explicit producer/consumer declaration | 1.0 | observed |
| Edge composed from exact prior evidence | 0.9 | inferred |
| Explicit Java event/command annotation | 1.0 | observed |
| Java event/command naming or location convention | 0.75 | inferred |
| Package namespace ownership indicator | 0.60 | inferred |
| Maven namespace ownership indicator | 0.65 | inferred |
| Dynamic property or expression | 0.65-0.70 | inferred with uncertainty |
| Candidate contract whose parse fails | 0.20 | observed file, incomplete content |

Exact source declarations remain observed even when the surrounding project
does not compile. A dynamic expression is preserved verbatim with an
`uncertainty` attribute rather than resolved from an environment.

## Partial Success

OpenAPI, AsyncAPI, JSON Schema, and Avro candidates are detected before deep
parsing. A malformed candidate produces:

- the valid document-marker evidence already available
- `contract-parse-error` evidence with an error code and recoverable flag
- a structured diagnostic containing the repository path
- plugin status `PARTIAL_SUCCESS`

Other files and plugins continue. Unreadable or syntactically imperfect Java
sources are handled by the existing lexical discovery behavior and diagnostics.

## AIM Mapping and Graph Policy

The standard `DiscoveryPluginAimMapper` maps all v0.2.4 evidence and
observations into AIM. Examples include:

- `ApplicationSubmitted v2 is defined in application-submitted-v2.json.`
- `LoanService publishes ApplicationSubmitted to mortgage-platform.`
- `RoutingPolicyHandler consumes ApplicationSubmitted.`
- `RoutingPolicyHandler references command RunCreditAssessmentCommand.`
- `credit-assessment-command is consumed by CreditAssessmentCommandConsumer.`
- `command contract RunCreditAssessment contains no explicit version declaration.`

Discovery does not mutate the canonical graph. Any inferred graph addition must
be represented later through the Proposed Change workflow and accepted through
normal governance.

## Known Limitations

- YAML/JSON parsing does not resolve remote `$ref` documents.
- OpenAPI correlation requires an exact static HTTP method and path.
- Custom Spring composed annotations are not resolved.
- Dynamic property, SpEL, environment, and runtime values remain unresolved.
- Java scanning is lexical and does not perform symbol or data-flow resolution.
- Producer payload extraction is limited to direct static type/name evidence.
- Infrastructure scanning recognizes straightforward CloudFormation/Terraform
  markers; it does not evaluate templates or provisioned cloud state.
- CODEOWNERS matching is deliberately conservative and does not settle conflicts.
- Compatibility metadata is recorded but compatibility is not evaluated.

## Why v0.2 Does Not Judge Routing Patterns

A central handler, router name, fan-out subscription, or multi-step message flow
is not sufficient proof of unhealthy orchestration or ESB drift. Such a
conclusion requires product boundaries, ownership, runtime behavior, change and
release evidence, and intended architecture. v0.2.4 supplies the traceable
communication facts. v0.3 may interpret those facts in product context and
propose governed changes.
