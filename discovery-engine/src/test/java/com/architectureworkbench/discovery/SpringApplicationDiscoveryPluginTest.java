package com.architectureworkbench.discovery;

import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Observation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringApplicationDiscoveryPluginTest {
    @TempDir Path root;

    @BeforeEach
    void createProject() throws IOException {
        Files.writeString(root.resolve("pom.xml"), "<project><modelVersion>4.0.0</modelVersion><groupId>com.example</groupId><artifactId>app</artifactId><version>1</version></project>");
        write("src/main/java/com/example/App.java", """
                package com.example;
                @SpringBootApplication
                @ComponentScan(basePackages = {"com.example.loan", "com.example.shared"})
                public class App {
                    public static void main(String[] args) { SpringApplication.run(App.class, args); }
                }
                """);
        write("src/main/java/com/example/LoanController.java", """
                package com.example;
                @RestController
                @RequestMapping("/applications")
                public class LoanController {
                    @Autowired
                    private LoanService fieldService;
                    private final LoanService loanService;
                    public LoanController(LoanService loanService) { this.loanService = loanService; }
                    @PostMapping
                    public ResponseEntity<ApplicationResponse> submit(@RequestBody ApplicationRequest request) { return null; }
                    @GetMapping("/{id}")
                    public ApplicationResponse get(UUID id) { return null; }
                }
                """);
        write("src/main/java/com/example/LoanService.java", """
                package com.example;
                @Service
                public class LoanService implements LoanUseCase {
                    @Autowired
                    private ApplicationRepository repository;
                    private AuditClient auditClient;
                    @Autowired public void setAuditClient(AuditClient auditClient) { this.auditClient = auditClient; }
                    public LoanService(ApplicationRepository repository) { this.repository = repository; }
                    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
                    public ApplicationResponse submit(ApplicationRequest request) { return null; }
                }
                """);
        write("src/main/java/com/example/AppConfiguration.java", """
                package com.example;
                @Configuration
                @Profile("prod")
                public class AppConfiguration {
                    @Bean
                    public Clock clock() { return Clock.systemUTC(); }
                }
                """);
        write("src/main/java/com/example/LoanApplication.java", """
                package com.example;
                @Entity @Table(name = "loan_application")
                public class LoanApplication {
                    @Id
                    private UUID id;
                }
                """);
        write("src/main/java/com/example/ApplicationRepository.java", """
                package com.example;
                public interface ApplicationRepository extends JpaRepository<LoanApplication, UUID> {
                    @Query("select a from LoanApplication a")
                    List<LoanApplication> findOpen();
                }
                """);
        write("src/main/java/com/example/ApplicationEventListener.java", """
                package com.example;
                @Component
                public class ApplicationEventListener {
                    private final ApplicationEventPublisher publisher;
                    public ApplicationEventListener(ApplicationEventPublisher publisher) { this.publisher = publisher; }
                    @EventListener
                    public void consume(ApplicationSubmitted event) {}
                    @KafkaListener(topics = "applications")
                    public void kafka(ApplicationSubmitted event) {}
                    public void publish(Object event) { publisher.publishEvent(event); }
                }
                """);
        write("src/main/java/com/example/ApiAdvice.java", """
                package com.example;
                @RestControllerAdvice public class ApiAdvice {}
                """);
        write("src/main/resources/application.yml", "spring:\n  profiles:\n    active: local\n");
        write("src/main/resources/application-prod.properties", "spring.profiles.include=secure\n");
        write("src/main/resources/bootstrap.yaml", "spring:\n  application:\n    name: loan\n");
    }

    @Test
    void discoversApplicationConfigurationAndProfiles() {
        DiscoveryPluginResult result = run(new SpringApplicationDiscoveryPlugin());
        assertAttribute(result, "spring-boot-application", "className", "App");
        assertAttribute(result, "spring-boot-entry-point", "methodName", "main");
        assertAttribute(result, "spring-component-scan-package", "scanBasePackage", "com.example.loan");
        assertAttribute(result, "spring-configuration-file", "configurationKind", "bootstrap");
        assertAttribute(result, "spring-profile", "profile", "prod");
        assertAttribute(result, "spring-profile", "profile", "local");
        assertAttribute(result, "spring-profile", "profile", "secure");
    }

    @Test
    void discoversControllersEndpointsDtosAdviceAndServiceDependencies() {
        DiscoveryPluginResult result = run(new SpringWebDiscoveryPlugin());
        assertAttribute(result, "spring-web-controller", "annotation", "RestController");
        assertAttribute(result, "spring-http-endpoint", "endpointPath", "/applications");
        assertAttribute(result, "spring-http-endpoint", "httpMethod", "POST");
        assertAttribute(result, "spring-http-endpoint", "endpointPath", "/applications/{id}");
        assertAttribute(result, "spring-web-dto-reference", "dtoType", "ApplicationRequest");
        assertAttribute(result, "spring-web-dto-reference", "dtoType", "ApplicationResponse");
        assertAttribute(result, "spring-controller-service-dependency", "dependencyType", "LoanService");
        assertAttribute(result, "spring-exception-advice", "annotation", "RestControllerAdvice");
    }

    @Test
    void discoversComponentsBeansAndInjection() {
        DiscoveryPluginResult result = run(new SpringComponentDiscoveryPlugin());
        assertAttribute(result, "spring-component", "componentKind", "service");
        assertAttribute(result, "spring-bean-method", "beanType", "Clock");
        assertAttribute(result, "spring-component-dependency", "injectionKind", "constructor");
        assertAttribute(result, "spring-component-dependency", "injectionKind", "field");
        assertAttribute(result, "spring-component-dependency", "injectionKind", "setter");
        assertAttribute(result, "spring-component-interface", "interfaceType", "LoanUseCase");
    }

    @Test
    void discoversDataAndTransactions() {
        DiscoveryPluginResult data = run(new SpringDataDiscoveryPlugin());
        assertAttribute(data, "spring-data-entity", "entityType", "LoanApplication");
        assertAttribute(data, "spring-data-table-mapping", "tableName", "loan_application");
        assertAttribute(data, "spring-data-entity-id", "idType", "UUID");
        assertAttribute(data, "spring-data-repository", "repositoryBase", "JpaRepository");
        assertAttribute(data, "spring-data-repository", "identifierType", "UUID");
        assertAttribute(data, "spring-data-explicit-query", "annotation", "Query");

        DiscoveryPluginResult transactions = run(new SpringTransactionDiscoveryPlugin());
        assertAttribute(transactions, "spring-transaction-boundary", "readOnly", "true");
        assertAttribute(transactions, "spring-transaction-boundary", "propagation", "REQUIRED");
        assertAttribute(transactions, "spring-transaction-boundary", "isolation", "READ_COMMITTED");
        assertAttribute(transactions, "spring-transaction-boundary", "componentKind", "service");
    }

    @Test
    void discoversMessagingListenersAndPublishers() {
        DiscoveryPluginResult result = run(new SpringMessagingDiscoveryPlugin());
        assertAttribute(result, "spring-message-listener", "listenerKind", "EventListener");
        assertAttribute(result, "spring-message-listener", "destination", "applications");
        assertAttribute(result, "spring-message-listener", "eventType", "ApplicationSubmitted");
        assertAttribute(result, "spring-message-publisher-reference", "publisherType", "ApplicationEventPublisher");
        assertAttribute(result, "spring-message-publisher-reference", "publishOperation", "publishEvent");
    }

    @Test
    void evidenceHasConfidenceProvenanceClassificationAndMapsToAim() {
        DiscoveryPluginResult result = run(new SpringWebDiscoveryPlugin());
        assertFalse(result.output().evidence().isEmpty());
        assertTrue(result.output().evidence().stream().allMatch(item -> item.confidence().value() > 0
                && item.provenance().contains(":line:")
                && item.attributes().containsKey("filePath")
                && item.attributes().containsKey("module")
                && item.attributes().containsKey("packageName")
                && item.attributes().containsKey("className")
                && item.attributes().containsKey("symbol")
                && item.attributes().containsKey("annotation")
                && item.attributes().get("pluginId").equals(SpringWebDiscoveryPlugin.ID.value())
                && Set.of("observed", "inferred").contains(item.attributes().get("classification"))));
        DiscoveryPluginAimMapper mapper = new DiscoveryPluginAimMapper();
        List<Evidence> mapped = result.output().evidence().stream().map(mapper::mapEvidence).toList();
        Observation observation = mapper.mapObservation(result.output().observations().stream()
                .filter(item -> item.observationType().equals("spring-http-endpoint-mapped")).findFirst().orElseThrow(), mapped);
        assertFalse(observation.relatedEvidence().isEmpty());
        assertTrue(observation.description().contains("POST /applications"));
    }

    @Test
    void metadataDeclaresFoundationDependencies() {
        for (DiscoveryPlugin plugin : plugins()) {
            assertEquals("0.2.3", plugin.metadata().version());
            assertTrue(plugin.metadata().deterministic());
            List<DiscoveryPluginId> ids = plugin.metadata().dependencies().stream().map(DiscoveryPluginDependency::pluginId).toList();
            assertTrue(ids.contains(RepositoryDiscoveryPlugin.ID));
            assertTrue(ids.contains(MavenDiscoveryPlugin.ID));
            assertTrue(ids.contains(JavaStructureDiscoveryPlugin.ID));
        }
    }

    @Test
    void handlesNonSpringAndMalformedSourceAndPreservesLegacyCompatibility() throws IOException {
        Path plain = root.resolve("plain");
        Files.createDirectories(plain.resolve("src/main/java/example"));
        Files.writeString(plain.resolve("src/main/java/example/Broken.java"), "package example; public class Broken {\n");
        for (DiscoveryPlugin plugin : plugins()) {
            DiscoveryPluginResult result = plugin.discover(DiscoveryInput.root(plain), context(plain));
            assertEquals(DiscoveryPluginStatus.SUCCEEDED, result.status());
        }
        DiscoveryResult legacy = new LocalRepositoryDiscoveryConnector().discover(new DiscoveryContext(
                DiscoveryRunId.newId(), source(root), root,
                new com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph("graph"), "architect"));
        assertTrue(legacy.artifacts().stream().anyMatch(item -> item.type() == DiscoveredArtifactType.SPRING_CONTROLLER
                && item.metadata().get("pluginId").equals(SpringWebDiscoveryPlugin.ID.value())));
        assertTrue(legacy.artifacts().stream().anyMatch(item -> item.type() == DiscoveredArtifactType.SPRING_SERVICE
                && item.metadata().get("pluginId").equals(SpringComponentDiscoveryPlugin.ID.value())));
        assertTrue(legacy.artifacts().stream().anyMatch(item -> item.type() == DiscoveredArtifactType.CONFIGURATION_FILE));
    }

    private DiscoveryPluginResult run(DiscoveryPlugin plugin) { return plugin.discover(DiscoveryInput.root(root), context(root)); }

    private static List<DiscoveryPlugin> plugins() {
        return List.of(new SpringApplicationDiscoveryPlugin(), new SpringWebDiscoveryPlugin(),
                new SpringComponentDiscoveryPlugin(), new SpringDataDiscoveryPlugin(),
                new SpringTransactionDiscoveryPlugin(), new SpringMessagingDiscoveryPlugin());
    }

    private static DiscoveryExecutionContext context(Path root) {
        return new DiscoveryExecutionContext(DiscoveryRunId.newId(), source(root), root, "architect", "correlation");
    }

    private static DiscoverySource source(Path root) {
        return new DiscoverySource("source", DiscoverySourceType.LOCAL_REPOSITORY, root.toUri().toString(), "source");
    }

    private void write(String relative, String content) throws IOException {
        Path file = root.resolve(relative); Files.createDirectories(file.getParent()); Files.writeString(file, content);
    }

    private static void assertAttribute(DiscoveryPluginResult result, String type, String name, String value) {
        assertTrue(result.output().evidence().stream().anyMatch(item -> item.evidenceType().equals(type)
                        && item.attributes().getOrDefault(name, "").equals(value)),
                () -> "Missing " + type + " with " + name + "=" + value + "; got " + result.output().evidence().stream()
                        .map(item -> item.evidenceType() + item.attributes()).toList());
    }
}
