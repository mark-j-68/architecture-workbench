# Spring Application Discovery

v0.2.3 adds six deterministic framework plugins to the Discovery Engine:

- `SpringApplicationDiscoveryPlugin` (`spring.application`)
- `SpringWebDiscoveryPlugin` (`spring.web`)
- `SpringComponentDiscoveryPlugin` (`spring.component`)
- `SpringDataDiscoveryPlugin` (`spring.data`)
- `SpringTransactionDiscoveryPlugin` (`spring.transaction`)
- `SpringMessagingDiscoveryPlugin` (`spring.messaging`)

They scan source and configuration as text. They never load classes, start a
Spring context, execute application code, resolve a database schema, or infer
an application architecture.

## Dependencies And Execution

Every plugin declares Repository Discovery, Maven Discovery, and Java Structure
Discovery in its metadata. Repository and Java Structure outputs are required
pipeline foundations. Maven is an optional enrichment so Gradle, incomplete,
and non-buildable repositories can still return evidence.

Each plugin is independently executable. Files are visited in stable
repository-relative path order, generated output directories are excluded, and
stable evidence identities are based on plugin, evidence type, path, symbol,
and source line.

## Supported Constructs

### Application And Configuration

Application discovery records `@SpringBootApplication`, `SpringApplication.run`
entry points, `@Configuration`, `@ComponentScan`, static scan base packages,
`@Profile`, Spring profile properties, and conventional `application` or
`bootstrap` `.properties`, `.yml`, and `.yaml` files. Profile-specific
filenames are also evidence.

### Web

Web discovery records `@RestController`, `@Controller`, controller advice, and
the standard request mapping annotations. It composes static class and method
paths and records the HTTP method and handler symbol. Endpoint signature types
provide narrow request/response DTO references. A controller constructor
parameter whose type ends in `Service` provides a direct type-based dependency
observation. The plugin does not generate or infer OpenAPI.

### Components And Injection

Component discovery records `@Service`, `@Component`, `@Repository`, and
`@Configuration` classes, `@Bean` methods, implemented interfaces, constructor
parameters, and straightforward field or setter injection marked by
`@Autowired`, `@Inject`, or `@Resource`.

### Spring Data

Data discovery records `@Entity`, `@Table`, `@Id`, explicit `@Query` methods,
and interfaces extending `JpaRepository`, `CrudRepository`,
`PagingAndSortingRepository`, or `MongoRepository`. Generic arguments provide
repository entity and identifier types and a narrow repository/entity
association. This is source structure evidence, not database schema evidence.

### Transactions

Transaction discovery records class-level and method-level `@Transactional`
boundaries and explicit `readOnly`, propagation, and isolation values. It also
records the annotated symbol and its directly observed Spring stereotype, when
present. Placement is not classified as healthy or unhealthy.

### Messaging

Messaging discovery records `@EventListener`,
`@TransactionalEventListener`, `@KafkaListener`, `@RabbitListener`, and
`@SqsListener`. Static listener destinations and method parameter event types
are retained. Direct references to `ApplicationEventPublisher`, `KafkaTemplate`,
`RabbitTemplate`, `SqsTemplate`, `EventBridgeClient`, `putEvents`, and static
builder destination/detail-type values are also evidence.

No ownership, orchestration, ESB, or bounded-context conclusions are emitted.

## Evidence Types

| Plugin | Representative evidence types |
| --- | --- |
| Application | `spring-boot-application`, `spring-boot-entry-point`, `spring-configuration-class`, `spring-component-scan`, `spring-component-scan-package`, `spring-profile`, `spring-configuration-file` |
| Web | `spring-web-controller`, `spring-http-endpoint`, `spring-web-dto-reference`, `spring-controller-service-dependency`, `spring-exception-advice` |
| Component | `spring-component`, `spring-bean-method`, `spring-component-dependency`, `spring-component-interface` |
| Data | `spring-data-entity`, `spring-data-table-mapping`, `spring-data-entity-id`, `spring-data-repository`, `spring-data-repository-entity-association`, `spring-data-explicit-query` |
| Transaction | `spring-transaction-boundary` |
| Messaging | `spring-message-listener`, `spring-message-publisher-reference`, `eventbridge-put-events-usage`, `messaging-static-destination` |

Every evidence item includes repository-relative file path, nearest Maven
module path or `.`, package, class and symbol where relevant, source line,
framework marker or annotation, plugin id, confidence with rationale, and an
`observed` or `inferred` classification.

## Confidence Rules

| Source fact | Confidence | Classification |
| --- | --- | --- |
| Explicit annotation, configuration file/property, type argument, or method call | `1.0` | Observed |
| Composed static endpoint path | `0.9` | Inferred |
| Dependency or DTO association derived from a declared type | `0.9` | Inferred |
| Dynamic `${...}`, `#{...}`, or concatenated value | `0.7` | Inferred with `dynamic-expression` uncertainty |

An inferred classification here means a narrow deterministic derivation, not
AI or architectural interpretation.

## Partial And Imperfect Sources

The scanner uses lightweight, comment-aware lexical parsing rather than a Java
compiler or type resolver. Non-Spring repositories succeed with empty Spring
evidence. Recognizable annotations and declarations in incomplete Java files
still produce partial evidence. An unreadable individual file becomes a
diagnostic while other files continue. Missing optional libraries do not affect
scanning because no application classes are loaded.

Known limitations include meta-annotations, annotation aliases, constants,
custom composed mappings, deeply nested annotation expressions, runtime
property resolution, Lombok-generated constructors, and symbol ownership. Such
cases require later parser or semantic enrichment and are not guessed.

## AIM And Legacy Connector Mapping

`DiscoveryPluginAimMapper` maps all Spring evidence and narrow observations to
AIM without creating findings, risks, recommendations, or proposed changes.
Descriptions stay factual, for example:

```text
POST /applications is handled by LoanController.submit.
LoanService has a constructor dependency on ApplicationRepository.
ApplicationService.submit is transactional.
ApplicationRepository manages LoanApplication using UUID identifiers.
ApplicationEventListener.consume consumes ApplicationSubmitted.
```

`LocalRepositoryDiscoveryConnector` delegates Spring scanning to these plugins.
It adapts controllers, services, repository components, and configuration files
to existing legacy artifact types. Evidence without a truthful legacy type
remains available through plugin output and AIM mapping.

## Interpretation Boundary

Spring framework evidence does not establish bounded contexts, architecture
style, modularity quality, distributed-monolith risk, event ownership,
orchestration, layering quality, or transaction health. Those conclusions, if
later implemented, belong to the Architecture Intelligence Engine and must cite
the underlying evidence.
