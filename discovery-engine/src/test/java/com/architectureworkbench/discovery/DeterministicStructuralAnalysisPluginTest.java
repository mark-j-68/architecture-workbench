package com.architectureworkbench.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Metric;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeterministicStructuralAnalysisPluginTest {
    @TempDir Path root;

    @Test void detectsDirectMultiPackageAndCrossModuleCyclesAndAcyclicGraphs() {
        DiscoveryOutput dependencies = output(
                edge("a", "b", "one", "one", "ab"), edge("b", "a", "one", "one", "ba"),
                edge("c", "d", "one", "two", "cd"), edge("d", "e", "two", "two", "de"), edge("e", "c", "two", "one", "ec"),
                edge("x", "y", "one", "two", "xy"));
        DiscoveryPluginResult result = run(new PackageCycleAnalysisPlugin(), dependencies);
        assertEquals(DiscoveryPluginStatus.SUCCEEDED, result.status());
        assertEquals(1, evidence(result, "direct-package-cycle").size());
        DiscoveryEvidence multi = only(result, "multi-package-cycle");
        assertEquals("true", multi.attributes().get("crossModule"));
        assertTrue(multi.attributes().get("edgeSequence").contains("c -> d"));
        assertFalse(result.output().observations().stream().anyMatch(o -> o.description().contains("violation")));
    }

    @Test void analyzesPackageEvidenceProducedFromATemporaryJavaProject() throws Exception {
        Path a = root.resolve("src/main/java/sample/a/A.java"); Path b = root.resolve("src/main/java/sample/b/B.java");
        Files.createDirectories(a.getParent()); Files.createDirectories(b.getParent());
        Files.writeString(a, "package sample.a;\nimport sample.b.B;\npublic class A { B b; }\n");
        Files.writeString(b, "package sample.b;\nimport sample.a.A;\npublic class B { A a; }\n");
        DiscoveryPluginResult java = run(new JavaStructureDiscoveryPlugin());
        DiscoveryPluginResult dependencies = run(new PackageDependencyDiscoveryPlugin(), java.output());
        DiscoveryPluginResult cycles = run(new PackageCycleAnalysisPlugin(), dependencies.output());
        assertTrue(java.output().evidence().stream().anyMatch(e -> e.evidenceType().equals("java-source-root")));
        assertEquals(2, dependencies.output().evidence().stream().filter(e -> e.evidenceType().equals("package-dependency")).count());
        assertEquals(1, evidence(cycles, "direct-package-cycle").size());
    }

    @Test void comparesModuleDirectionsAndDetectsBidirectionalReferences() {
        DiscoveryEvidence oneTwo = evidence("module-package-reference", "one->two", Map.of("sourceModule", "one", "targetModule", "two"));
        DiscoveryEvidence twoOne = evidence("module-package-reference", "two->one", Map.of("sourceModule", "two", "targetModule", "one"));
        DiscoveryEvidence moduleOne = evidenceAt("build-module", "module-one", "one/pom.xml", Map.of("artifactId", "one"));
        DiscoveryEvidence moduleTwo = evidenceAt("build-module", "module-two", "two/pom.xml", Map.of("artifactId", "two"));
        DiscoveryEvidence moduleThree = evidenceAt("build-module", "module-three", "three/pom.xml", Map.of("artifactId", "three"));
        DiscoveryEvidence declaredOnly = evidenceAt("dependency-declaration", "one->three", "one/pom.xml", Map.of("artifactId", "three"));
        DiscoveryPluginResult result = run(new ModuleDependencyAnalysisPlugin(), output(oneTwo, twoOne, moduleOne, moduleTwo, moduleThree, declaredOnly));
        assertEquals(1, evidence(result, "bidirectional-module-reference").size());
        assertTrue(evidence(result, "observed-use-without-declaration").size() >= 2);
        assertEquals(1, evidence(result, "declared-without-observed-use").size());
        assertTrue(evidence(result, "module-fan-in").stream().allMatch(e -> !e.references().isEmpty()));
    }

    @Test void infersCandidateLayersAndPreservesUnusualDirectionsAsObservations() {
        DiscoveryEvidence controller = evidence("spring-web-controller", "LoanController", Map.of("className", "LoanController", "packageName", "app.controller"));
        DiscoveryEvidence service = evidence("spring-component", "LoanService", Map.of("className", "LoanService", "componentKind", "service"));
        DiscoveryEvidence repository = evidence("spring-data-repository", "LoanRepository", Map.of("className", "LoanRepository"));
        DiscoveryEvidence entity = evidence("spring-data-entity", "Loan", Map.of("className", "Loan"));
        DiscoveryEvidence controllerService = evidence("spring-controller-service-dependency", "LoanController->LoanService", Map.of("className", "LoanController", "dependencyType", "LoanService"));
        DiscoveryEvidence controllerRepository = evidence("spring-component-dependency", "LoanController->LoanRepository", Map.of("className", "LoanController", "dependencyType", "LoanRepository"));
        DiscoveryEvidence repositoryEntity = evidence("spring-data-repository-entity-association", "LoanRepository->Loan", Map.of("repositoryType", "LoanRepository", "entityType", "Loan"));
        DiscoveryEvidence packageCandidate = evidence("java-package", "app.adapter", Map.of("packageName", "app.adapter"));
        DiscoveryEvidence controllerTransaction = evidence("spring-transaction-boundary", "LoanController.submit", Map.of("symbol", "LoanController.submit", "componentKind", "controller"));
        DiscoveryPluginResult result = run(new LayerStructureAnalysisPlugin(), output(controller, service, repository, entity,
                controllerService, controllerRepository, repositoryEntity, packageCandidate, controllerTransaction));
        assertTrue(evidence(result, "candidate-layer").stream().anyMatch(e -> e.attributes().get("candidateLayer").equals("adapter") && e.confidence().value() == 0.7));
        assertEquals(1, observations(result, "controller-depends-on-service").size());
        assertEquals(1, observations(result, "controller-directly-depends-on-repository").size());
        assertEquals(1, observations(result, "repository-depends-on-domain-entity").size());
        assertEquals(1, evidence(result, "controller-transaction-boundary").size());
    }

    @Test void calculatesComponentFanInFanOutPathsAndBidirectionality() {
        DiscoveryEvidence a = evidence("spring-component", "AService", Map.of("className", "AService", "componentKind", "service"));
        DiscoveryEvidence b = evidence("spring-component", "BService", Map.of("className", "BService", "componentKind", "service"));
        DiscoveryEvidence c = evidence("spring-component", "CService", Map.of("className", "CService", "componentKind", "service"));
        DiscoveryOutput source = output(a, b, c,
                evidence("spring-component-dependency", "A->B", Map.of("className", "AService", "dependencyType", "BService")),
                evidence("spring-component-dependency", "B->A", Map.of("className", "BService", "dependencyType", "AService")),
                evidence("spring-component-dependency", "C->B", Map.of("className", "CService", "dependencyType", "BService")));
        DiscoveryPluginResult result = run(new ComponentDependencyAnalysisPlugin(), source);
        assertEquals("2", evidence(result, "component-fan-in").stream().filter(e -> e.attributes().get("component").equals("BService")).findFirst().orElseThrow().attributes().get("value"));
        assertEquals(3, evidence(result, "component-dependency-path").size());
        assertEquals(1, evidence(result, "bidirectional-component-dependency").size());
    }

    @Test void comparesExactContractVersionsAndReportsMissingEvidence() {
        DiscoveryOutput source = output(
                version("ApplicationSubmitted", "2"), producer("ApplicationSubmitted", "2", "p2"), consumer("ApplicationSubmitted", "2", "c2"),
                version("RunCredit", "1"), producer("RunCredit", "1", "p1"), consumer("RunCredit", "2", "c1"),
                evidence("event-contract", "NoVersion", Map.of("contractId", "NoVersion")), producer("NoVersion", "", "pn"), consumer("NoVersion", "", "cn"));
        DiscoveryPluginResult result = run(new ContractVersionAnalysisPlugin(), source);
        assertEquals(1, evidence(result, "contract-version-exact-match").size());
        assertEquals(1, evidence(result, "contract-version-explicit-mismatch").size());
        assertTrue(evidence(result, "contract-version-not-detected").stream().anyMatch(e -> e.attributes().get("contractId").equals("NoVersion")));
        assertTrue(evidence(result, "contract-version-compatibility-unresolved").size() >= 1);
    }

    @Test void calculatesRoutesCentralNodesMissingPeersAndInfrastructurePresence() {
        DiscoveryOutput source = output(
                topology("topology-producer-channel", "P1", "events", "PRODUCES_TO", "p1"),
                topology("topology-producer-channel", "P2", "events", "PRODUCES_TO", "p2"),
                topology("topology-channel-consumer", "events", "RoutingPolicyHandler", "CONSUMED_BY", "c1"),
                topology("topology-event-handler", "EventOne", "RoutingPolicyHandler", "HANDLED_BY", "h1"),
                topology("topology-event-handler", "EventTwo", "RoutingPolicyHandler", "HANDLED_BY", "h2"),
                topology("topology-handler-command", "RoutingPolicyHandler", "RunCreditCommand", "PUBLISHES_COMMAND", "hc"),
                topology("topology-producer-channel", "OrphanProducer", "orphan", "PRODUCES_TO", "op"),
                topology("topology-channel-consumer", "empty", "OrphanConsumer", "CONSUMED_BY", "oc"),
                evidence("messaging-infrastructure-topology", "dlq", Map.of("topologyKind", "dead-letter-queue")));
        DiscoveryPluginResult result = run(new MessagingTopologyAnalysisPlugin(), source);
        assertEquals(2, evidence(result, "event-to-command-path").size());
        assertEquals("2", only(result, "central-routing-node").attributes().get("routeCount"));
        assertTrue(evidence(result, "channel-without-detected-consumer").stream().anyMatch(e -> e.identity().equals("orphan")));
        assertTrue(evidence(result, "consumer-without-detected-producer").stream().anyMatch(e -> e.identity().equals("OrphanConsumer")));
        assertEquals(1, evidence(result, "topology-dead-letter-queue-present").size());
    }

    @Test void boundedTopologyTraversalReturnsPartialSuccessAndWarning() {
        List<DiscoveryEvidence> edges = new ArrayList<>();
        for (int i = 0; i < StructuralAnalysisSupport.MAX_PATHS + 1; i++) {
            edges.add(topology("topology-event-handler", "Event", "Handler" + i, "HANDLED_BY", "h" + i));
            edges.add(topology("topology-handler-command", "Handler" + i, "Command" + i, "PUBLISHES_COMMAND", "c" + i));
        }
        DiscoveryPluginResult result = run(new MessagingTopologyAnalysisPlugin(), output(edges.toArray(DiscoveryEvidence[]::new)));
        assertEquals(DiscoveryPluginStatus.PARTIAL_SUCCESS, result.status());
        assertTrue(result.output().diagnostics().stream().anyMatch(d -> d.contains("limit")));
        assertEquals(StructuralAnalysisSupport.MAX_PATHS, evidence(result, "event-to-command-path").size());
    }

    @Test void metricsAreTraceableAndMapToAimMetric() {
        DiscoveryEvidence pkg = evidence("java-package", "app", Map.of("packageName", "app"));
        DiscoveryEvidence clazz = evidence("java-class", "App", Map.of("className", "App"));
        DiscoveryEvidence contract = evidence("event-contract", "Event", Map.of("contractId", "Event"));
        DiscoveryEvidence version = version("Event", "1");
        DiscoveryPluginResult result = run(new DependencyMetricsPlugin(), output(pkg, clazz, contract, version));
        DiscoveryEvidence packageMetric = evidence(result, "structural-metric").stream().filter(e -> e.attributes().get("metricName").equals("package-count")).findFirst().orElseThrow();
        assertEquals("1.0", packageMetric.attributes().get("value")); assertFalse(packageMetric.references().isEmpty());
        assertEquals(root.toAbsolutePath().toString(), packageMetric.attributes().get("workspace"));
        StructuralMetric typed = new StructuralMetric("package-count", 1, "count", List.of(pkg.evidenceId()), "Counted packages.");
        DiscoveryPluginAimMapper mapper = new DiscoveryPluginAimMapper(); Evidence mapped = mapper.mapEvidence(pkg); Metric aim = mapper.mapMetric(typed, List.of(mapped));
        assertEquals(1, aim.value()); assertEquals("package-count", aim.name()); assertNotNull(aim.trend());
    }

    @Test void nonSpringNonMessagingAndIncompleteEvidenceSucceedWithZeroMetrics() {
        DiscoveryPluginResult layer = run(new LayerStructureAnalysisPlugin(), output(evidence("java-class", "Plain", Map.of("className", "Plain"))));
        DiscoveryPluginResult topology = run(new MessagingTopologyAnalysisPlugin(), output());
        DiscoveryPluginResult metrics = run(new DependencyMetricsPlugin(), output());
        assertEquals(DiscoveryPluginStatus.SUCCEEDED, layer.status()); assertTrue(layer.output().observations().isEmpty());
        assertEquals(DiscoveryPluginStatus.SUCCEEDED, topology.status()); assertTrue(evidence(topology, "event-to-command-path").isEmpty());
        assertTrue(evidence(metrics, "structural-metric").stream().anyMatch(e -> e.attributes().get("metricName").equals("package-count") && e.attributes().get("value").equals("0.0")));
    }

    @Test void metadataIsDeterministicAndDoesNotAdvertiseProductReasoning() {
        List<DiscoveryPlugin> plugins = List.of(new PackageCycleAnalysisPlugin(), new ModuleDependencyAnalysisPlugin(), new LayerStructureAnalysisPlugin(),
                new ComponentDependencyAnalysisPlugin(), new ContractVersionAnalysisPlugin(), new MessagingTopologyAnalysisPlugin(), new DependencyMetricsPlugin());
        assertTrue(plugins.stream().allMatch(p -> p.metadata().deterministic() && p.metadata().version().equals("0.2.5")));
        assertTrue(plugins.stream().allMatch(p -> p.metadata().category().equals("Structural Analysis Plugin")));
    }

    private DiscoveryPluginResult run(DiscoveryPlugin plugin, DiscoveryOutput... outputs) {
        DiscoverySource source = new DiscoverySource("test", DiscoverySourceType.LOCAL_REPOSITORY, root.toString(), "test");
        DiscoveryExecutionContext context = new DiscoveryExecutionContext(DiscoveryRunId.newId(), source, root, "test", "test");
        return plugin.discover(DiscoveryInput.root(root).withPriorOutputs(List.of(outputs)), context);
    }
    private DiscoveryEvidence edge(String from, String to, String fromModule, String toModule, String id) { return evidence("package-dependency", id, Map.of("sourcePackage", from, "targetPackage", to, "sourceModule", fromModule, "targetModule", toModule, "dependencyKind", "internal")); }
    private DiscoveryEvidence version(String contract, String version) { return evidence("contract-version", contract + version, Map.of("contractId", contract, "contractVersion", version)); }
    private DiscoveryEvidence producer(String contract, String version, String id) { return evidence("event-producer-reference", id, Map.of("contractId", contract, "contractVersion", version, "producer", id)); }
    private DiscoveryEvidence consumer(String contract, String version, String id) { return evidence("event-consumer-reference", id, Map.of("contractId", contract, "contractVersion", version, "consumer", id)); }
    private DiscoveryEvidence topology(String type, String from, String to, String relationship, String id) { return evidence(type, id, Map.of("from", from, "to", to, "relationship", relationship, "channelName", relationship.equals("PRODUCES_TO") ? to : from)); }
    private DiscoveryEvidence evidence(String type, String identity, Map<String, String> attributes) { return new DiscoveryEvidence("e-" + identity, type, "test", "synthetic", identity, DiscoveryConfidence.observedFact("synthetic"), true, Instant.EPOCH, List.of("source.java"), attributes); }
    private DiscoveryEvidence evidenceAt(String type, String identity, String reference, Map<String, String> attributes) { return new DiscoveryEvidence("e-" + identity, type, "test", "synthetic", identity, DiscoveryConfidence.observedFact("synthetic"), true, Instant.EPOCH, List.of(reference), attributes); }
    private static DiscoveryOutput output(DiscoveryEvidence... evidence) { return new DiscoveryOutput(List.of(evidence), List.of(), List.of()); }
    private static List<DiscoveryEvidence> evidence(DiscoveryPluginResult result, String type) { return result.output().evidence().stream().filter(e -> e.evidenceType().equals(type)).toList(); }
    private static DiscoveryEvidence only(DiscoveryPluginResult result, String type) { return evidence(result, type).getFirst(); }
    private static List<DiscoveryObservation> observations(DiscoveryPluginResult result, String type) { return result.output().observations().stream().filter(e -> e.observationType().equals(type)).toList(); }
}
