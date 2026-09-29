package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.*;
import com.architectureworkbench.audit.InMemoryAuditSink;
import com.architectureworkbench.workspace.FileWorkspaceIntegrityService;
import com.architectureworkbench.workspace.WorkspaceId;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProductScorecardServiceTest {
    @TempDir Path root;

    @Test void scoresOnlySupportedFindingsAndRetainsHistoricalSnapshots() throws Exception {
        String w = "workspace", p = "product";
        Files.createDirectories(root.resolve(w));
        var store = new FileProductRepositoryStore(root, new ObjectMapper());
        var product = product(w, p);
        store.save(product);
        var audit = new InMemoryAuditSink();
        var service = new ProductScorecardService(store, audit);
        assertThrows(IllegalStateException.class, () -> service.generate(w, p));
        store.saveArchitectureAnalysis(w, p, analysis(w, p, "analysis-1", 80,
                finding("cycle", "CROSS_REPOSITORY_CYCLE", "RISK", "HIGH", .8)));
        var first = service.generate(w, p);
        assertEquals(26, first.metrics().stream().filter(m -> m.metric().equals("REPOSITORY_INDEPENDENCE")).findFirst().orElseThrow().score());
        assertNull(first.metrics().stream().filter(m -> m.metric().equals("ARCHITECTURE_DRIFT")).findFirst().orElseThrow().score());
        assertTrue(first.measuredWeight() < 50);
        assertNull(first.overallScore());
        assertEquals("INSUFFICIENT_EVIDENCE", first.status());
        store.saveArchitectureAnalysis(w, p, analysis(w, p, "analysis-2", 80,
                finding("release", "RELEASE_LOCKSTEP", "RISK", "HIGH", .9),
                finding("deploy", "DEPLOYMENT_LOCKSTEP", "RISK", "HIGH", .9),
                finding("contract", "CONTRACT_IMMATURITY", "RISK", "MEDIUM", .9),
                finding("module", "PRODUCT_MODULE_BOUNDARY_MISMATCH", "RISK", "LOW", .9),
                finding("cycle-2", "CROSS_REPOSITORY_CYCLE", "RISK", "HIGH", .9)));
        var second = service.generate(w, p);
        assertNotNull(second.overallScore());
        assertEquals("PARTIAL", second.status());
        assertEquals("NO_COMPARABLE_SCORE", second.trend());
        assertEquals(first.scorecardId(), second.previousScorecardId());
        assertEquals(2, service.history(w, p).size());
        assertEquals(first, service.get(w, p, first.scorecardId()));
        var reloaded = new ProductScorecardService(new FileProductRepositoryStore(root, new ObjectMapper()), audit);
        assertEquals(second, reloaded.latest(w, p));
        assertTrue(new FileWorkspaceIntegrityService(root).verifyWorkspace(WorkspaceId.of(w)).valid());
        assertEquals(product, store.find(w, p).orElseThrow());
        assertTrue(audit.entries().stream().anyMatch(e -> e.action().equals("ProductScorecardGenerated")));
    }

    @Test void coverageAndCounterEvidenceDoNotCreateAnArbitraryScore() {
        String w = "w", p = "p";
        var store = new InMemoryProductRepositoryStore();
        store.save(product(w, p));
        store.saveArchitectureAnalysis(w, p, analysis(w, p, "analysis-1", 20,
                finding("release", "RELEASE_LOCKSTEP", "RISK", "HIGH", .9)));
        var card = new ProductScorecardService(store, new InMemoryAuditSink()).generate(w, p);
        assertNull(card.overallScore());
        assertEquals(0, card.metrics().stream().filter(m -> m.score() == null).findFirst().orElseThrow().confidence());
        assertTrue(card.metrics().stream().filter(m -> m.metric().equals("RELEASE_INDEPENDENCE")).findFirst().orElseThrow().confidence() < .8);
    }

    @Test void linksRecommendationMetadataWithoutChangingGovernanceState() {
        String w = "w", p = "p";
        var store = new InMemoryProductRepositoryStore();
        var audit = new InMemoryAuditSink();
        var product = product(w, p);
        store.save(product);
        store.saveArchitectureAnalysis(w, p, analysis(w, p, "analysis-1", 80,
                finding("release", "RELEASE_LOCKSTEP", "RISK", "HIGH", .9)));
        var recommendations = new ProductArchitectureRecommendationService(store, audit);
        var candidate = recommendations.generate(w, p, "architect").recommendations().getFirst();
        recommendations.linkReview(w, p, candidate.recommendationId(), "review-1", new RecommendationActionRequest("architect", "review"));
        recommendations.transition(w, p, candidate.recommendationId(), "UNDER_REVIEW", new RecommendationActionRequest("architect", "vote"));
        var card = new ProductScorecardService(store, audit).generate(w, p);
        var release = card.metrics().stream().filter(m -> m.metric().equals("RELEASE_INDEPENDENCE")).findFirst().orElseThrow();
        assertEquals(List.of(candidate.recommendationId()), release.recommendationIds());
        assertEquals(List.of("UNDER_REVIEW"), release.reviewStatuses());
        assertEquals("UNDER_REVIEW", recommendations.get(w, p, candidate.recommendationId()).status());
        assertEquals(product, store.find(w, p).orElseThrow());
    }

    @Test void retainsReviewAndLifecycleSnapshotsInFileWorkspaceHistory() throws Exception {
        String w = "review-workspace", p = "review-product";
        Files.createDirectories(root.resolve(w));
        var store = new FileProductRepositoryStore(root, new ObjectMapper());
        store.save(product(w, p));
        var session = new ReviewBoardSessionResponse("session-1", "graph-1", "correlation-1", "OPEN",
                List.of("recommendation-1"), List.of(),
                List.of(new ReviewBoardParticipantResponse("architect", "Architect", "HUMAN_ARCHITECT")),
                List.of(), null);
        var snapshot = new ProductReviewSnapshot("snapshot-1", w, p, "recommendation-1", "analysis-1", 1,
                List.of("repository-1"), List.of("run-1"), List.of("finding-1"), List.of("evidence-1"),
                "SUBMITTED", session, "", Instant.now());
        var event = new ProductRecommendationLifecycleEvent("event-1", "recommendation-1", "SUBMITTED",
                "session-1", "", "architect", "review", Instant.now());
        store.saveReviewSnapshot(w, p, snapshot);
        store.saveRecommendationLifecycle(w, p, event);
        var reloaded = new FileProductRepositoryStore(root, new ObjectMapper());
        assertEquals(List.of(snapshot), reloaded.reviewSnapshots(w, p));
        assertEquals(List.of(event), reloaded.recommendationLifecycle(w, p));
        assertTrue(new FileWorkspaceIntegrityService(root).verifyWorkspace(WorkspaceId.of(w)).valid());
    }

    private ProductModels.Product product(String w, String p) {
        return new ProductModels.Product(new ProductModels.ProductId(p), w,
                new ProductModels.ProductName("Product"), new ProductModels.ProductDescription(""),
                ProductModels.ProductStatus.ACTIVE, List.of(), List.of(),
                new ProductModels.ProductCompositionVersion(1), Instant.now(), Instant.now());
    }

    private ProductArchitectureFindingView finding(String id, String type, String polarity, String severity, double confidence) {
        return new ProductArchitectureFindingView(id, type, polarity, type, type, "MODULARITY", severity, "HIGH", confidence,
                List.of(), List.of(), List.of("evidence-" + id), List.of(), List.of(), List.of(),
                "deterministic analysis", List.of(), List.of(), 1, Instant.now());
    }

    private ProductArchitectureAnalysisView analysis(String w, String p, String id, int coverage, ProductArchitectureFindingView... findings) {
        var assessment = new ProductDistributedMonolithAssessment("assessment", p, 1, "COMPLETED", "COUPLED", "HIGH", .8,
                coverage, List.of(), List.of(), List.of(), List.of(), List.of(), Instant.now());
        return new ProductArchitectureAnalysisView(id, p, w, 1, "COMPLETED", Instant.now(), Instant.now(), id,
                List.of(findings), assessment, List.of());
    }
}
