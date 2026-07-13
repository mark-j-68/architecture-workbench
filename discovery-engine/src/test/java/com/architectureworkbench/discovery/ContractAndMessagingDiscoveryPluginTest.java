package com.architectureworkbench.discovery;

import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Observation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractAndMessagingDiscoveryPluginTest {
    @TempDir Path root;

    @BeforeEach void createRepository() throws IOException {
        write("pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>com.acme.loan</groupId><artifactId>loan-service</artifactId><version>1</version></project>");
        write("contracts/openapi.yaml", """
                openapi: 3.1.0
                info:
                  title: Loan API
                  version: 2.4.0
                  contact:
                    name: Lending Team
                servers:
                  - url: https://api.example.test
                  - url: ${API_SERVER}
                paths:
                  /applications:
                    post:
                      operationId: submitApplication
                      requestBody:
                        content:
                          application/json:
                            schema:
                              $ref: '#/components/schemas/ApplicationRequest'
                      responses:
                        '202':
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/ApplicationResponse'
                  /old:
                    get:
                      deprecated: true
                      responses: { '204': { description: old } }
                components:
                  schemas:
                    ApplicationRequest: { type: object }
                    ApplicationResponse: { type: object }
                  securitySchemes:
                    bearerAuth: { type: http, scheme: bearer }
                """);
        write("contracts/broken-openapi.yaml", "openapi: 3.1.0\ninfo:\n  title: Broken\npaths: [\n");
        write("contracts/admin-openapi.json", """
                {"openapi":"3.0.3","info":{"title":"Admin API","version":"1.0.0"},"paths":{"/health":{"get":{"responses":{"200":{"description":"ok"}}}}}}
                """);
        write("contracts/events/application-submitted-v2.json", """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "title": "ApplicationSubmitted",
                  "x-event-type": "ApplicationSubmitted",
                  "version": "2",
                  "x-owner": "lending-events",
                  "compatibility": "BACKWARD",
                  "properties": {
                    "eventType": {"const": "ApplicationSubmitted"},
                    "payload": {"type": "object"}
                  }
                }
                """);
        write("contracts/commands/run-credit-assessment.json", """
                {
                  "title": "RunCreditAssessment",
                  "x-command-type": "RunCreditAssessment",
                  "queueName": "credit-assessment-command",
                  "replyQueue": "credit-assessment-reply",
                  "properties": {
                    "correlationId": {"type": "string"},
                    "causationId": {"type": "string"},
                    "payload": {"type": "object"}
                  }
                }
                """);
        write("contracts/asyncapi.yaml", """
                asyncapi: 2.6.0
                info:
                  title: Lending Events
                  version: 1.1.0
                  contact: { name: Integration Team }
                channels:
                  applications:
                    publish:
                      operationId: publishApplication
                      message: { $ref: '#/components/messages/ApplicationSubmitted' }
                    subscribe:
                      operationId: consumeApplication
                      message: { $ref: '#/components/messages/ApplicationSubmitted' }
                components:
                  messages:
                    ApplicationSubmitted:
                      x-version: '2'
                      payload: { type: object }
                """);
        write("src/main/java/com/acme/LoanController.java", """
                package com.acme;
                @RestController
                @RequestMapping("/applications")
                public class LoanController {
                    @PostMapping
                    public ApplicationResponse submit(ApplicationRequest request) { return null; }
                }
                """);
        write("src/main/java/com/acme/events/ApplicationSubmittedEvent.java", """
                package com.acme.events;
                @EventContract
                public record ApplicationSubmittedEvent(String applicationId, int version) {}
                """);
        write("src/main/java/com/acme/commands/RunCreditAssessmentCommand.java", """
                package com.acme.commands;
                public record RunCreditAssessmentCommand(String correlationId, String causationId) {}
                """);
        write("src/main/java/com/acme/RoutingPolicyHandler.java", """
                package com.acme;
                @Component
                public class RoutingPolicyHandler {
                    private final SqsClient sqs;
                    private final EventBridgeClient eventBridge;
                    @EventListener
                    public void handle(ApplicationSubmitted event) {
                        var command = new RunCreditAssessmentCommand("c", "e");
                        sqs.sendMessage(r -> r.queue("credit-assessment-command"));
                    }
                    public void publish(String detail) {
                        eventBridge.putEvents(r -> r.detailType("ApplicationSubmitted")
                            .eventBusName("mortgage-platform").source("loan-service"));
                    }
                }
                """);
        write("src/main/java/com/acme/CreditAssessmentCommandConsumer.java", """
                package com.acme;
                public class CreditAssessmentCommandConsumer {
                    @SqsListener("credit-assessment-command")
                    public void consume(RunCreditAssessmentCommand command) {}
                }
                """);
        write("src/main/java/com/acme/DynamicKafkaConsumer.java", """
                package com.acme;
                public class DynamicKafkaConsumer {
                    @KafkaListener(topics = "${application.topic}")
                    public void consume(ApplicationSubmitted event) {}
                }
                """);
        write("infra/messaging.yml", """
                Resources:
                  CommandQueue:
                    Type: AWS::SQS::Queue
                    Properties:
                      RedrivePolicy:
                        deadLetterTargetArn: command-dlq
                  RetryQueue:
                    retry-queue: command-retry
                  Archive:
                    Type: AWS::Events::Archive
                    Properties:
                      ArchiveName: application-archive
                  Fanout:
                    Type: AWS::SNS::Subscription
                """);
        write(".github/CODEOWNERS", "/contracts/events/ @events-team\n/contracts/openapi.yaml @api-team\n");
        write("contracts/README.md", "Owner: Contract Guild\n");
    }

    @Test void discoversValidOpenApiAndConservativelyCorrelatesSpringEndpoints() {
        List<DiscoveryOutput> foundations = foundations();
        DiscoveryPluginResult result = run(new OpenApiContractDiscoveryPlugin(), foundations);
        assertAttribute(result, "openapi-document", "openApiVersion", "3.1.0");
        assertAttribute(result, "api-contract", "apiTitle", "Loan API");
        assertAttribute(result, "api-contract", "apiTitle", "Admin API");
        assertAttribute(result, "api-operation-contract", "httpMethod", "POST");
        assertAttribute(result, "api-operation-contract", "requestSchemas", "ApplicationRequest");
        assertAttribute(result, "api-operation-contract", "responseSchemas", "ApplicationResponse");
        assertAttribute(result, "api-operation-contract", "springCorrelation", "true");
        assertAttribute(result, "spring-openapi-endpoint-correlation", "classification", "inferred");
        assertAttribute(result, "api-component-schema", "schemaName", "ApplicationResponse");
        assertAttribute(result, "api-security-scheme", "schemeName", "bearerAuth");
        assertAttribute(result, "api-operation-contract", "deprecated", "true");
        assertTrue(result.output().evidence().stream().anyMatch(item -> item.evidenceType().equals("api-server")
                && item.attributes().get("uncertainty").equals("dynamic-expression") && item.confidence().value() < 1.0));
    }

    @Test void invalidOpenApiReturnsPartialSuccessAndPreservesValidEvidence() {
        DiscoveryPluginResult result = run(new OpenApiContractDiscoveryPlugin(), foundations());
        assertEquals(DiscoveryPluginStatus.PARTIAL_SUCCESS, result.status());
        assertType(result, "api-contract");
        assertType(result, "contract-parse-error");
        assertTrue(result.output().diagnostics().stream().anyMatch(value -> value.startsWith("INVALID_CONTRACT_DOCUMENT|")));
    }

    @Test void discoversJsonSchemaAvroAsyncApiAndJavaEventContracts() {
        writeUnchecked("contracts/events/payment-authorised.avsc", """
                {"type":"record","name":"PaymentAuthorisedEvent","namespace":"com.acme.events","fields":[{"name":"version","type":"int"}]}
                """);
        DiscoveryPluginResult result = run(new EventContractDiscoveryPlugin(), foundations());
        assertAttribute(result, "event-contract", "eventName", "ApplicationSubmitted");
        assertAttribute(result, "event-contract", "contractVersion", "2");
        assertAttribute(result, "event-contract", "schemaFormat", "avro");
        assertAttribute(result, "event-contract", "versionField", "true");
        assertAttribute(result, "event-envelope", "envelope", "true");
        assertAttribute(result, "asyncapi-document", "asyncApiVersion", "2.6.0");
        assertAttribute(result, "message-channel", "channelName", "applications");
        assertType(result, "asyncapi-producer");
        assertType(result, "asyncapi-consumer");
        assertAttribute(result, "java-event-contract", "eventName", "ApplicationSubmittedEvent");
    }

    @Test void discoversCommandEnvelopeSqsProducerConsumerAndEventBridgeProducer() {
        DiscoveryPluginResult event = run(new EventContractDiscoveryPlugin(), foundations());
        assertAttribute(event, "event-producer-reference", "detailType", "ApplicationSubmitted");
        assertAttribute(event, "event-producer-reference", "channelName", "mortgage-platform");

        DiscoveryPluginResult command = run(new CommandContractDiscoveryPlugin(), foundations());
        assertAttribute(command, "command-contract", "commandName", "RunCreditAssessment");
        assertAttribute(command, "command-envelope", "correlationField", "true");
        assertAttribute(command, "java-command-contract", "classification", "inferred");
        assertAttribute(command, "command-producer-reference", "queueName", "credit-assessment-command");
        assertAttribute(command, "command-consumer-reference", "queueName", "credit-assessment-command");
    }

    @Test void buildsEventHandlerCommandQueueConsumerAndInfrastructureTopology() {
        List<DiscoveryOutput> outputs = contractOutputs();
        DiscoveryPluginResult result = run(new MessagingTopologyDiscoveryPlugin(), outputs);
        assertAttribute(result, "topology-producer-channel", "to", "mortgage-platform");
        assertAttribute(result, "topology-event-handler", "from", "ApplicationSubmitted");
        assertAttribute(result, "topology-handler-command", "to", "RunCreditAssessmentCommand");
        assertAttribute(result, "topology-producer-channel", "to", "credit-assessment-command");
        assertAttribute(result, "topology-channel-consumer", "from", "credit-assessment-command");
        assertAttribute(result, "messaging-infrastructure-topology", "topologyKind", "dead-letter-queue");
        assertAttribute(result, "messaging-infrastructure-topology", "topologyKind", "retry-queue");
        assertAttribute(result, "messaging-infrastructure-topology", "topologyKind", "archive-replay");
        assertAttribute(result, "messaging-infrastructure-topology", "topologyKind", "sns-sqs-fanout");
        assertTrue(result.output().evidence().stream().filter(item -> item.evidenceType().startsWith("topology-"))
                .allMatch(item -> !item.attributes().get("sourceEvidenceIds").isBlank()));
    }

    @Test void reportsExplicitAndMissingVersionsWithoutRiskFindings() {
        DiscoveryPluginResult result = run(new ContractVersionDiscoveryPlugin(), contractOutputs());
        assertAttribute(result, "contract-version", "contractVersion", "2.4.0");
        assertAttribute(result, "contract-version", "contractVersion", "2");
        assertAttribute(result, "contract-compatibility", "compatibility", "BACKWARD");
        assertTrue(result.output().observations().stream().anyMatch(item -> item.observationType().equals("contract-version-absent")
                && item.description().contains("RunCreditAssessment")));
        assertTrue(result.output().evidence().stream().noneMatch(item -> item.evidenceType().contains("finding") || item.evidenceType().contains("risk")));
    }

    @Test void collectsCodeownersMetadataDocumentationMavenAndNamespaceIndicators() {
        DiscoveryPluginResult result = run(new ContractOwnershipEvidencePlugin(), contractOutputs());
        assertAttribute(result, "contract-ownership-evidence", "owner", "@api-team");
        assertAttribute(result, "contract-ownership-evidence", "owner", "@events-team");
        assertAttribute(result, "contract-ownership-evidence", "owner", "lending-events");
        assertAttribute(result, "contract-ownership-evidence", "owner", "Contract Guild");
        assertAttribute(result, "contract-ownership-evidence", "ownershipSource", "maven-coordinate");
        assertTrue(result.output().evidence().stream().filter(item -> item.attributes().getOrDefault("owner", "").equals("@api-team"))
                .allMatch(DiscoveryEvidence::directlyObserved));
    }

    @Test void evidenceHasProvenanceConfidenceSourceIdsAndMapsIntoAim() {
        DiscoveryPluginResult result = run(new MessagingTopologyDiscoveryPlugin(), contractOutputs());
        assertFalse(result.output().evidence().isEmpty());
        assertTrue(result.output().evidence().stream().allMatch(item -> item.provenance().contains(":line:")
                && item.confidence().value() > 0 && item.attributes().containsKey("filePath")
                && item.attributes().containsKey("module") && item.attributes().containsKey("symbol")
                && item.attributes().get("pluginId").equals(MessagingTopologyDiscoveryPlugin.ID.value())
                && Set.of("observed", "inferred").contains(item.attributes().get("classification"))));
        DiscoveryPluginAimMapper mapper = new DiscoveryPluginAimMapper();
        List<Evidence> mapped = result.output().evidence().stream().map(mapper::mapEvidence).toList();
        Observation mappedObservation = mapper.mapObservation(result.output().observations().get(0), mapped);
        assertFalse(mappedObservation.relatedEvidence().isEmpty());
    }

    @Test void metadataDeclaresDependenciesAndTypedValueObjectsEnforceIntent() {
        for (DiscoveryPlugin plugin : contractPlugins()) {
            assertEquals("0.2.4", plugin.metadata().version());
            assertTrue(plugin.metadata().deterministic());
            assertFalse(plugin.metadata().dependencies().isEmpty());
        }
        ContractEndpoint endpoint = new ContractEndpoint(ContractId.of("loan-api"), "/applications", "submit", "post");
        assertEquals("POST", endpoint.method());
        assertEquals(MessageChannelType.QUEUE, new MessageChannel("commands", MessageChannelType.QUEUE, false).type());
    }

    @Test void handlesNonMessagingRepositoriesAndPreservesLegacyConnectorBehaviour() throws IOException {
        Path plain = root.resolve("plain");
        Files.createDirectories(plain.resolve("src/main/java/example"));
        Files.writeString(plain.resolve("src/main/java/example/Broken.java"), "package example; class Broken {");
        for (DiscoveryPlugin plugin : contractPlugins()) {
            DiscoveryPluginResult result = plugin.discover(DiscoveryInput.root(plain), context(plain));
            assertTrue(Set.of(DiscoveryPluginStatus.SUCCEEDED, DiscoveryPluginStatus.PARTIAL_SUCCESS).contains(result.status()));
        }
        DiscoveryResult legacy = new LocalRepositoryDiscoveryConnector().discover(new DiscoveryContext(
                DiscoveryRunId.newId(), source(root), root,
                new com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph("graph"), "architect"));
        assertTrue(legacy.artifacts().stream().anyMatch(item -> item.type() == DiscoveredArtifactType.SPRING_CONTROLLER
                && item.metadata().get("pluginId").equals(SpringWebDiscoveryPlugin.ID.value())));
    }

    private List<DiscoveryOutput> foundations() {
        List<DiscoveryOutput> outputs = new ArrayList<>();
        outputs.add(run(new RepositoryDiscoveryPlugin(), outputs).output());
        outputs.add(run(new MavenDiscoveryPlugin(), outputs).output());
        outputs.add(run(new JavaStructureDiscoveryPlugin(), outputs).output());
        outputs.add(run(new SpringWebDiscoveryPlugin(), outputs).output());
        outputs.add(run(new SpringMessagingDiscoveryPlugin(), outputs).output());
        return outputs;
    }

    private List<DiscoveryOutput> contractOutputs() {
        List<DiscoveryOutput> outputs = new ArrayList<>(foundations());
        outputs.add(run(new OpenApiContractDiscoveryPlugin(), outputs).output());
        outputs.add(run(new EventContractDiscoveryPlugin(), outputs).output());
        outputs.add(run(new CommandContractDiscoveryPlugin(), outputs).output());
        return outputs;
    }

    private DiscoveryPluginResult run(DiscoveryPlugin plugin, List<DiscoveryOutput> prior) {
        return plugin.discover(DiscoveryInput.root(root).withPriorOutputs(prior), context(root));
    }

    private static List<DiscoveryPlugin> contractPlugins() {
        return List.of(new OpenApiContractDiscoveryPlugin(), new EventContractDiscoveryPlugin(),
                new CommandContractDiscoveryPlugin(), new MessagingTopologyDiscoveryPlugin(),
                new ContractVersionDiscoveryPlugin(), new ContractOwnershipEvidencePlugin());
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

    private void writeUnchecked(String relative, String content) {
        try { write(relative, content); } catch (IOException exception) { throw new AssertionError(exception); }
    }

    private static void assertType(DiscoveryPluginResult result, String type) {
        assertTrue(result.output().evidence().stream().anyMatch(item -> item.evidenceType().equals(type)),
                () -> "Missing " + type + "; got " + result.output().evidence().stream().map(DiscoveryEvidence::evidenceType).toList());
    }

    private static void assertAttribute(DiscoveryPluginResult result, String type, String name, String value) {
        assertTrue(result.output().evidence().stream().anyMatch(item -> item.evidenceType().equals(type)
                        && item.attributes().getOrDefault(name, "").equals(value)),
                () -> "Missing " + type + " with " + name + "=" + value + "; got " + result.output().evidence().stream()
                        .map(item -> item.evidenceType() + item.attributes()).toList());
    }
}
