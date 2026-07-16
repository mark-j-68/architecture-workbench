package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.CloseReviewBoardSessionRequest;
import com.architectureworkbench.api.ApiDtos.CreateWorkspaceRequest;
import com.architectureworkbench.api.ApiDtos.DecideProposedChangeRequest;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunResponse;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunDetails;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunSummary;
import com.architectureworkbench.api.ApiDtos.DiscoveryEvidenceView;
import com.architectureworkbench.api.ApiDtos.DiscoveryObservationView;
import com.architectureworkbench.api.ApiDtos.DiscoveryMetricView;
import com.architectureworkbench.api.ApiDtos.DiscoveryDiagnosticView;
import com.architectureworkbench.api.ApiDtos.FindingResponse;
import com.architectureworkbench.api.ApiDtos.GenerateProjectionRequest;
import com.architectureworkbench.api.ApiDtos.GraphResponse;
import com.architectureworkbench.api.ApiDtos.OpenReviewBoardSessionRequest;
import com.architectureworkbench.api.ApiDtos.ProjectionResponse;
import com.architectureworkbench.api.ApiDtos.ProposedChangeResponse;
import com.architectureworkbench.api.ApiDtos.RecordReviewBoardVoteRequest;
import com.architectureworkbench.api.ApiDtos.RecommendationResponse;
import com.architectureworkbench.api.ApiDtos.ReviewBoardParticipantRequest;
import com.architectureworkbench.api.ApiDtos.ReviewBoardSessionResponse;
import com.architectureworkbench.api.ApiDtos.WorkspaceResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "architecture.workbench.persistence=in-memory")
@AutoConfigureMockMvc
class ArchitectureApiIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @Test
    void exposesKernelWorkflowThroughThinApiShell() throws Exception {
        createSampleSpringProject(tempDir);

        WorkspaceResponse workspace = postJson(
                "/api/workspaces",
                new CreateWorkspaceRequest("API Workspace", "architect"),
                WorkspaceResponse.class,
                HttpStatus.CREATED
        );
        assertFalse(workspace.id().isBlank());

        List<WorkspaceResponse> workspaces = getList("/api/workspaces", new TypeReference<>() {});
        assertTrue(workspaces.stream().anyMatch(candidate -> candidate.id().equals(workspace.id())));

        GraphResponse initialGraph = getJson("/api/workspaces/" + workspace.id() + "/graph", GraphResponse.class);
        assertTrue(initialGraph.elements().isEmpty());

        DiscoveryRunResponse discoveryRun = postJson(
                "/api/workspaces/" + workspace.id() + "/discovery/local",
                new ApiDtos.RunLocalDiscoveryRequest(tempDir.toString(), "architect"),
                DiscoveryRunResponse.class,
                HttpStatus.OK
        );
        assertTrue(discoveryRun.findingCount() > 0);
        assertTrue(discoveryRun.recommendationCount() > 0);
        assertTrue(discoveryRun.proposedChangeCount() > 2);

        List<FindingResponse> findings = getList(
                "/api/workspaces/" + workspace.id() + "/discovery/runs/" + discoveryRun.runId() + "/findings",
                new TypeReference<>() {}
        );
        List<RecommendationResponse> recommendations = getList(
                "/api/workspaces/" + workspace.id() + "/discovery/runs/" + discoveryRun.runId() + "/recommendations",
                new TypeReference<>() {}
        );
        List<ProposedChangeResponse> proposedChanges = getList(
                "/api/workspaces/" + workspace.id() + "/discovery/runs/" + discoveryRun.runId() + "/proposed-changes",
                new TypeReference<>() {}
        );
        assertFalse(findings.isEmpty());
        assertFalse(recommendations.isEmpty());
        assertFalse(proposedChanges.isEmpty());
        assertEquals(recommendations.get(0).id(), proposedChanges.get(0).recommendationId());

        ReviewBoardSessionResponse session = postJson(
                "/api/workspaces/" + workspace.id() + "/review-board/sessions",
                new OpenReviewBoardSessionRequest(
                        List.of(recommendations.get(0).id()),
                        List.of(proposedChanges.get(0).id()),
                        List.of(
                                new ReviewBoardParticipantRequest("architect", "Lead Architect", "HUMAN_ARCHITECT"),
                                new ReviewBoardParticipantRequest("ddd", "DDD Reviewer", "DDD_REVIEWER")
                        ),
                        "architect"
                ),
                ReviewBoardSessionResponse.class,
                HttpStatus.CREATED
        );
        session = postJson(
                "/api/review-board/sessions/" + session.sessionId() + "/votes",
                new RecordReviewBoardVoteRequest("architect", "APPROVE", "Ready to accept."),
                ReviewBoardSessionResponse.class,
                HttpStatus.OK
        );
        session = postJson(
                "/api/review-board/sessions/" + session.sessionId() + "/votes",
                new RecordReviewBoardVoteRequest("ddd", "APPROVE", "Boundary is acceptable."),
                ReviewBoardSessionResponse.class,
                HttpStatus.OK
        );
        session = postJson(
                "/api/review-board/sessions/" + session.sessionId() + "/close",
                new CloseReviewBoardSessionRequest("architect"),
                ReviewBoardSessionResponse.class,
                HttpStatus.OK
        );
        assertEquals("ACCEPT_PROPOSED_CHANGE", session.decision().decisionType());

        GraphResponse graphAfterReview = getJson("/api/workspaces/" + workspace.id() + "/graph", GraphResponse.class);
        assertTrue(graphAfterReview.elements().isEmpty());

        ProposedChangeResponse accepted = postJson(
                "/api/proposed-changes/" + proposedChanges.get(0).id() + "/accept",
                new DecideProposedChangeRequest(workspace.id(), "architect", "Accepted after review board approval."),
                ProposedChangeResponse.class,
                HttpStatus.OK
        );
        assertEquals("ACCEPTED", accepted.status());

        ProposedChangeResponse rejected = postJson(
                "/api/proposed-changes/" + proposedChanges.get(1).id() + "/reject",
                new DecideProposedChangeRequest(workspace.id(), "architect", "Rejected as duplicate."),
                ProposedChangeResponse.class,
                HttpStatus.OK
        );
        ProposedChangeResponse deferred = postJson(
                "/api/proposed-changes/" + proposedChanges.get(2).id() + "/defer",
                new DecideProposedChangeRequest(workspace.id(), "architect", "Deferred pending evidence."),
                ProposedChangeResponse.class,
                HttpStatus.OK
        );
        assertEquals("REJECTED", rejected.status());
        assertEquals("DEFERRED", deferred.status());

        GraphResponse graphAfterAcceptance = getJson("/api/workspaces/" + workspace.id() + "/graph", GraphResponse.class);
        assertEquals(1, graphAfterAcceptance.elements().size());

        ProjectionResponse projection = postJson(
                "/api/workspaces/" + workspace.id() + "/projections",
                new GenerateProjectionRequest("REACT_FLOW", "architect"),
                ProjectionResponse.class,
                HttpStatus.OK
        );
        assertEquals("REACT_FLOW", projection.type());
        assertEquals(1, projection.sourceElementRefs().size());
        assertNotNull(projection.payload());
    }

    @Test
    void exposesInspectableDeterministicDiscoveryRunsAndFilters() throws Exception {
        createSampleSpringProject(tempDir);
        WorkspaceResponse workspace = postJson("/api/workspaces", new CreateWorkspaceRequest("Evidence Workspace", "architect"),
                WorkspaceResponse.class, HttpStatus.CREATED);
        DiscoveryRunDetails run = postJson("/api/workspaces/" + workspace.id() + "/discovery-runs",
                new ApiDtos.RunLocalDiscoveryRequest(tempDir.toString(), "architect"), DiscoveryRunDetails.class, HttpStatus.CREATED);

        assertEquals("COMPLETED", run.summary().status());
        assertTrue(run.summary().pluginExecutionCount() > 20);
        assertFalse(run.evidence().isEmpty());
        assertFalse(run.observations().isEmpty());
        assertFalse(run.metrics().isEmpty());
        assertTrue(run.plugins().stream().allMatch(plugin -> plugin.startedAt() != null && plugin.completedAt() != null));
        assertTrue(run.evidence().stream().allMatch(item -> item.confidence() != null && item.provenance() != null));

        List<DiscoveryRunSummary> history = getList("/api/workspaces/" + workspace.id() + "/discovery-runs", new TypeReference<>() {});
        assertTrue(history.stream().anyMatch(item -> item.runId().equals(run.summary().runId())));
        DiscoveryRunDetails reloaded = getJson("/api/workspaces/" + workspace.id() + "/discovery-runs/" + run.summary().runId(), DiscoveryRunDetails.class);
        assertEquals(run.summary().evidenceCount(), reloaded.evidence().size());

        List<DiscoveryEvidenceView> packages = getList("/api/workspaces/" + workspace.id() + "/discovery-runs/" + run.summary().runId()
                + "/evidence?evidenceType=java-package&minimumConfidence=0.9", new TypeReference<>() {});
        assertFalse(packages.isEmpty());
        assertTrue(packages.stream().allMatch(item -> item.type().equals("java-package") && item.confidence().value() >= .9));
        List<DiscoveryObservationView> observations = getList("/api/workspaces/" + workspace.id() + "/discovery-runs/" + run.summary().runId()
                + "/observations?supportingEvidenceId=" + packages.getFirst().evidenceId(), new TypeReference<>() {});
        assertTrue(observations.stream().allMatch(item -> item.supportingEvidenceIds().contains(packages.getFirst().evidenceId())));
        List<DiscoveryMetricView> metrics = getList("/api/workspaces/" + workspace.id() + "/discovery-runs/" + run.summary().runId() + "/metrics", new TypeReference<>() {});
        List<DiscoveryDiagnosticView> diagnostics = getList("/api/workspaces/" + workspace.id() + "/discovery-runs/" + run.summary().runId() + "/diagnostics", new TypeReference<>() {});
        assertFalse(metrics.isEmpty());
        assertEquals(run.summary().warningCount() + run.summary().failureCount(), diagnostics.size());
    }

    @Test
    void exposesProductCompositionWithoutReplacingDiscoveryApis() throws Exception {
        createSampleSpringProject(tempDir);
        WorkspaceResponse workspace = postJson("/api/workspaces", new CreateWorkspaceRequest("Product Workspace", "architect"), WorkspaceResponse.class, HttpStatus.CREATED);
        DiscoveryRunDetails run = postJson("/api/workspaces/" + workspace.id() + "/discovery-runs", new ApiDtos.RunLocalDiscoveryRequest(tempDir.toString(), "architect"), DiscoveryRunDetails.class, HttpStatus.CREATED);
        ApiDtos.ProductView product = postJson("/api/workspaces/" + workspace.id() + "/products", new ApiDtos.CreateProductRequest("Mortgage", "Composition test", "architect"), ApiDtos.ProductView.class, HttpStatus.CREATED);
        product = postJson("/api/workspaces/" + workspace.id() + "/products/" + product.productId() + "/repositories", new ApiDtos.AddProductRepositoryRequest("sample", tempDir.toString(), "SERVICE", List.of(run.summary().runId()), java.util.Map.of(), java.util.Map.of("team", "mortgage"), "architect"), ApiDtos.ProductView.class, HttpStatus.CREATED);
        assertEquals(1, product.repositories().size());
        ApiDtos.ProductCompositionView composition = postJson("/api/workspaces/" + workspace.id() + "/products/" + product.productId() + "/compose", java.util.Map.of(), ApiDtos.ProductCompositionView.class, HttpStatus.OK);
        assertFalse(composition.evidence().isEmpty());
        assertEquals(run.summary().runId(), composition.evidence().getFirst().discoveryRunId());
        assertEquals(1, getList("/api/workspaces/" + workspace.id() + "/products", new TypeReference<List<ApiDtos.ProductView>>() {}).size());
        assertEquals(composition.metrics(), getJson("/api/workspaces/" + workspace.id() + "/products/" + product.productId() + "/metrics", ApiDtos.ProductCompositionMetrics.class));
    }

    private <T> T getJson(String url, Class<T> responseType) throws Exception {
        String content = mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(content, responseType);
    }

    private <T> T postJson(String url, Object request, Class<T> responseType, HttpStatus expectedStatus) throws Exception {
        String content = mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus.value()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(content, responseType);
    }

    private <T> List<T> getList(String url, TypeReference<List<T>> responseType) throws Exception {
        String content = mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(content, responseType);
    }

    private static void createSampleSpringProject(Path root) throws IOException {
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
        Files.writeString(root.resolve("README.md"), "# API Test Project\n");
        Files.writeString(root.resolve("docs/overview.md"), "# Overview\n");
        Files.writeString(root.resolve("Dockerfile"), "FROM eclipse-temurin:21\n");
        Files.writeString(root.resolve("app/src/main/resources/application.yml"), "server:\n  port: 8080\n");
        Files.writeString(root.resolve("app/src/main/java/com/example/app/api/CustomerController.java"), """
                package com.example.app.api;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class CustomerController {}
                """);
        Files.writeString(root.resolve("app/src/main/java/com/example/app/service/CustomerService.java"), """
                package com.example.app.service;
                import org.springframework.stereotype.Service;
                @Service
                class CustomerService {}
                """);
        Files.writeString(root.resolve("app/src/main/java/com/example/app/repository/CustomerRepository.java"), """
                package com.example.app.repository;
                class CustomerRepository {}
                """);
        Files.writeString(root.resolve("domain/src/main/java/com/example/domain/Customer.java"), """
                package com.example.domain;
                class Customer {}
                """);
    }
}
