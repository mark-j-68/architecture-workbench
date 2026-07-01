package com.architectureworkbench.discovery;

import com.architectureworkbench.audit.InMemoryAuditSink;
import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ArchitectureElementType;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.ImmutableKnowledgeGraphAuditLog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryEngineTest {
    @TempDir
    Path tempDir;

    @Test
    void localRepositoryConnectorDiscoversMavenSpringDockerDocsAndTests() throws IOException {
        createRepresentativeProject(tempDir);
        DiscoveryContext context = context(tempDir, new ArchitectureKnowledgeGraph("discovery-kg"));

        DiscoveryResult result = new LocalRepositoryDiscoveryConnector().discover(context);

        assertTrue(has(result, DiscoveredArtifactType.POM_FILE));
        assertTrue(has(result, DiscoveredArtifactType.MAVEN_MODULE));
        assertTrue(has(result, DiscoveredArtifactType.JAVA_PACKAGE));
        assertTrue(has(result, DiscoveredArtifactType.SPRING_CONTROLLER));
        assertTrue(has(result, DiscoveredArtifactType.SPRING_SERVICE));
        assertTrue(has(result, DiscoveredArtifactType.REPOSITORY_CLASS));
        assertTrue(has(result, DiscoveredArtifactType.CONFIGURATION_FILE));
        assertTrue(has(result, DiscoveredArtifactType.DOCKER_FILE));
        assertTrue(has(result, DiscoveredArtifactType.README_DOC));
        assertTrue(has(result, DiscoveredArtifactType.DOC_FILE));
        assertTrue(has(result, DiscoveredArtifactType.ADR_DIRECTORY));
        assertTrue(has(result, DiscoveredArtifactType.TEST_DIRECTORY));
    }

    @Test
    void discoveryServiceMapsArtifactsIntoGraphAndEmitsAuditEvents() throws IOException {
        createRepresentativeProject(tempDir);
        InMemoryAuditSink auditSink = new InMemoryAuditSink();
        ArchitectureKnowledgeGraph graph = new ArchitectureKnowledgeGraph("discovery-kg");
        DiscoveryService service = service(auditSink);

        DiscoveryRun run = service.runDiscovery(context(tempDir, graph));

        assertTrue(run.artifacts().size() > 5);
        assertTrue(graph.elements().isEmpty());
        assertEquals(run.artifacts().size(), run.evidence().stream()
                .filter(evidence -> evidence.source().startsWith("discovery:"))
                .count());
        assertEquals(run.artifacts().size(), run.observations().size());
        assertTrue(run.aimFindings().size() >= run.artifacts().size());
        assertFalse(run.recommendations().isEmpty());
        assertFalse(run.proposedChanges().isEmpty());
        assertEquals(run.recommendations().get(0).id(), run.proposedChanges().get(0).recommendationId());
        assertTrue(run.proposedChanges().get(0).findingIds().stream()
                .anyMatch(findingId -> run.aimFindings().stream().anyMatch(finding -> finding.id().equals(findingId))));
        assertTrue(run.proposedChanges().get(0).evidenceIds().stream()
                .anyMatch(evidenceId -> run.evidence().stream().anyMatch(evidence -> evidence.id().equals(evidenceId))));
        assertEquals("discovery-kg", run.proposedChanges().get(0).workspaceId());
        assertFalse(run.proposedChanges().get(0).correlationId().value().isBlank());
        assertTrue(run.observations().get(0).relatedEvidence().contains(run.evidence().get(0)));
        assertTrue(run.aimFindings().stream()
                .anyMatch(finding -> finding.supportingObservations().stream()
                        .anyMatch(observation -> observation.relatedEvidence().stream()
                                .anyMatch(evidence -> evidence.supportingArtifacts().contains(run.artifacts().get(0).artifactId())))));
        assertTrue(auditSink.entries().stream().anyMatch(event -> event.action().equals("DiscoveryStarted")));
        assertTrue(auditSink.entries().stream().anyMatch(event -> event.action().equals("DISCOVERY_ARTIFACT_DISCOVERED")));
        assertTrue(auditSink.entries().stream().anyMatch(event -> event.action().equals("DiscoveryCompleted")));
        assertTrue(auditSink.entries().stream()
                .filter(event -> event.action().equals("DiscoveryStarted"))
                .allMatch(event -> event.architectureEvent().workspaceId().equals("discovery-kg")
                        && !event.architectureEvent().correlationId().value().isBlank()));
        assertTrue(auditSink.entries().stream()
                .filter(event -> event.action().equals("DiscoveryCompleted"))
                .allMatch(event -> event.architectureEvent().mutationTarget().name().equals("BOTH")));
    }

    @Test
    void healthchecksDetectMissingArchitectureSignals() throws IOException {
        Files.createDirectories(tempDir.resolve("src/main/java/com/example/app"));
        Files.writeString(tempDir.resolve("pom.xml"), "<project></project>");

        InMemoryAuditSink auditSink = new InMemoryAuditSink();
        DiscoveryContext context = context(tempDir, new ArchitectureKnowledgeGraph("sparse-kg"));
        DiscoveryResult result = new LocalRepositoryDiscoveryConnector().discover(context);

        List<DiscoveryFinding> findings = new HealthcheckService(auditSink).runHealthchecks(context, result);

        assertTrue(findings.stream().anyMatch(finding -> finding.ruleId().equals("DISC-README-001")));
        assertTrue(findings.stream().anyMatch(finding -> finding.ruleId().equals("DISC-TEST-001")));
        assertTrue(findings.stream().anyMatch(finding -> finding.ruleId().equals("DISC-ADR-001")));
        assertTrue(findings.stream().anyMatch(finding -> finding.ruleId().equals("DISC-API-001")));
        assertTrue(findings.stream().anyMatch(finding -> finding.ruleId().equals("DISC-DOMAIN-001")));
        assertEquals(findings.size(), auditSink.entries().stream().filter(event -> event.action().equals("HEALTHCHECK_FINDING_CREATED")).count());
    }

    @Test
    void healthchecksReportPositiveArchitectureSignals() throws IOException {
        createRepresentativeProject(tempDir);
        InMemoryAuditSink auditSink = new InMemoryAuditSink();
        DiscoveryContext context = context(tempDir, new ArchitectureKnowledgeGraph("signals-kg"));
        DiscoveryResult result = new LocalRepositoryDiscoveryConnector().discover(context);

        List<DiscoveryFinding> findings = new HealthcheckService(auditSink).runHealthchecks(context, result);

        assertTrue(findings.stream().anyMatch(finding -> finding.ruleId().equals("DISC-MODULE-001")));
        assertTrue(findings.stream().anyMatch(finding -> finding.ruleId().equals("DISC-SPRING-CTRL-001")));
        assertTrue(findings.stream().anyMatch(finding -> finding.ruleId().equals("DISC-DOCKER-001")));
    }

    private static DiscoveryService service(InMemoryAuditSink auditSink) {
        var graphAudit = new ImmutableKnowledgeGraphAuditLog();
        var elementService = new ArchitectureElementService(graphAudit);
        return new DiscoveryService(
                List.of(new LocalRepositoryDiscoveryConnector()),
                new DiscoveryGraphMapper(elementService),
                new HealthcheckService(auditSink),
                auditSink
        );
    }

    private static DiscoveryContext context(Path root, ArchitectureKnowledgeGraph graph) {
        return new DiscoveryContext(
                DiscoveryRunId.newId(),
                new DiscoverySource("local-test-source", DiscoverySourceType.LOCAL_REPOSITORY, root.toUri().toString(), "Local Test Source"),
                root,
                graph,
                "architect"
        );
    }

    private static boolean has(DiscoveryResult result, DiscoveredArtifactType type) {
        return result.artifacts().stream().anyMatch(artifact -> artifact.type() == type);
    }

    private static void createRepresentativeProject(Path root) throws IOException {
        Files.writeString(root.resolve("pom.xml"), """
                <project>
                  <modules>
                    <module>app</module>
                    <module>domain</module>
                  </modules>
                </project>
                """);
        Files.createDirectories(root.resolve("app/src/main/java/com/example/app/api"));
        Files.createDirectories(root.resolve("app/src/main/java/com/example/app/service"));
        Files.createDirectories(root.resolve("app/src/main/java/com/example/app/repository"));
        Files.createDirectories(root.resolve("app/src/main/resources"));
        Files.createDirectories(root.resolve("app/src/test/java/com/example/app"));
        Files.createDirectories(root.resolve("domain/src/main/java/com/example/domain"));
        Files.createDirectories(root.resolve("architecture/adr"));
        Files.createDirectories(root.resolve("docs"));
        Files.writeString(root.resolve("app/pom.xml"), "<project></project>");
        Files.writeString(root.resolve("domain/pom.xml"), "<project></project>");
        Files.writeString(root.resolve("README.md"), "# Test Project\n");
        Files.writeString(root.resolve("docs/overview.md"), "# Overview\n");
        Files.writeString(root.resolve("Dockerfile"), "FROM eclipse-temurin:21\n");
        Files.writeString(root.resolve("app/src/main/resources/application.yml"), "server:\n  port: 8080\n");
        Files.writeString(root.resolve("app/src/main/java/com/example/app/api/MortgageController.java"), """
                package com.example.app.api;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class MortgageController {}
                """);
        Files.writeString(root.resolve("app/src/main/java/com/example/app/service/MortgageService.java"), """
                package com.example.app.service;
                import org.springframework.stereotype.Service;
                @Service
                class MortgageService {}
                """);
        Files.writeString(root.resolve("app/src/main/java/com/example/app/repository/MortgageRepository.java"), """
                package com.example.app.repository;
                class MortgageRepository {}
                """);
        Files.writeString(root.resolve("domain/src/main/java/com/example/domain/MortgageApplication.java"), """
                package com.example.domain;
                class MortgageApplication {}
                """);
    }
}
