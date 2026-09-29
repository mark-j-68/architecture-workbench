package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.*;
import com.architectureworkbench.audit.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/** A retained projection of deterministic Product analysis, never canonical architecture state. */
final class ProductScorecardService {
    private record Dimension(String id, String label, int weight, Set<String> indicators) {}
    private static final List<Dimension> DIMENSIONS = List.of(
            new Dimension("PRODUCT_MODULARITY", "Product modularity", 15, Set.of("PRODUCT_MODULE_BOUNDARY_MISMATCH", "PRODUCT_PACKAGING_COUPLING", "INDEPENDENT_MODULES")),
            new Dimension("REPOSITORY_INDEPENDENCE", "Repository independence", 10, Set.of("CROSS_REPOSITORY_CYCLE", "SHARED_DOMAIN_MODEL", "ACYCLIC_DEPENDENCIES")),
            new Dimension("RELEASE_INDEPENDENCE", "Release independence", 10, Set.of("RELEASE_LOCKSTEP", "INDEPENDENT_VERSIONING")),
            new Dimension("DEPLOYMENT_INDEPENDENCE", "Deployment independence", 10, Set.of("DEPLOYMENT_LOCKSTEP")),
            new Dimension("CONTRACT_MATURITY", "Contract maturity", 10, Set.of("CONTRACT_IMMATURITY", "EXPLICIT_CONTRACT_OWNERSHIP", "VERSION_COVERAGE")),
            new Dimension("BOUNDED_CONTEXT_COHESION", "Bounded context cohesion", 10, Set.of()),
            new Dimension("COUPLING", "Coupling", 10, Set.of("CROSS_REPOSITORY_CYCLE", "SHARED_DOMAIN_MODEL", "SHARED_DATABASE_OR_SCHEMA", "SYNCHRONOUS_COUPLING", "ACYCLIC_DEPENDENCIES")),
            new Dimension("COMMUNICATION_COMPLEXITY", "Communication complexity", 5, Set.of("SYNCHRONOUS_COUPLING", "MESSAGING_COUPLING", "CENTRAL_ROUTER_HIGH_COORDINATION_LOAD", "CENTRAL_ROUTER_ESB_DRIFT_RISK")),
            new Dimension("OWNERSHIP_CLARITY", "Ownership clarity", 5, Set.of("OWNERSHIP_COUPLING", "EXPLICIT_CONTRACT_OWNERSHIP")),
            new Dimension("OPERATIONAL_COMPLEXITY", "Operational complexity", 5, Set.of("DEPLOYMENT_LOCKSTEP", "CENTRAL_ROUTER_HIGH_COORDINATION_LOAD")),
            new Dimension("ARCHITECTURE_DRIFT", "Architecture drift", 5, Set.of()),
            new Dimension("DISTRIBUTED_MONOLITH_RISK", "Distributed monolith risk", 5, Set.of("RELEASE_LOCKSTEP", "DEPLOYMENT_LOCKSTEP", "CROSS_REPOSITORY_CYCLE", "SHARED_DOMAIN_MODEL", "SHARED_DATABASE_OR_SCHEMA", "SYNCHRONOUS_COUPLING", "ACYCLIC_DEPENDENCIES", "INDEPENDENT_VERSIONING"))
    );
    private static final Set<String> VALIDATED = Set.of("APPROVED", "PROPOSED_CHANGE", "ARCHITECTURE_CHANGE_ACCEPTED", "IMPLEMENTED", "VERIFIED");
    private final ProductRepositoryStore store;
    private final AuditSink audit;

    ProductScorecardService(ProductRepositoryStore store, AuditSink audit) { this.store = store; this.audit = audit; }

    ProductScorecardView generate(String workspaceId, String productId) {
        requireProduct(workspaceId, productId);
        var analysis = store.architectureAnalysis(workspaceId, productId)
                .orElseThrow(() -> new IllegalStateException("Run Product architecture analysis before generating a scorecard."));
        var recommendations = store.recommendationGenerations(workspaceId, productId).stream()
                .filter(g -> g.analysisId().equals(analysis.analysisId()))
                .flatMap(g -> g.recommendations().stream()).toList();
        var previous = latestOrNull(workspaceId, productId);
        Instant now = Instant.now();
        List<ProductScoreMetricView> metrics = new ArrayList<>();
        for (var dimension : DIMENSIONS) {
            var findings = analysis.findings().stream().filter(f -> dimension.indicators().contains(f.findingType())).toList();
            var linked = recommendations.stream().filter(r -> findings.stream().anyMatch(f -> r.supportingFindingIds().contains(f.findingId()))).toList();
            Integer score = findings.isEmpty() ? null : score(findings);
            double confidence = findings.isEmpty() ? 0 : confidence(analysis, findings, linked, now);
            var old = previous == null ? null : previous.metrics().stream().filter(m -> m.metric().equals(dimension.id())).findFirst().orElse(null);
            metrics.add(new ProductScoreMetricView(dimension.id(), dimension.label(), dimension.weight(), score, confidence,
                    findings.stream().map(ProductArchitectureFindingView::findingId).toList(),
                    findings.stream().flatMap(f -> f.supportingEvidenceIds().stream()).distinct().sorted().toList(),
                    linked.stream().map(ProductArchitectureRecommendationView::recommendationId).distinct().toList(),
                    linked.stream().map(ProductArchitectureRecommendationView::status).distinct().toList(),
                    score == null ? "Unscored: no deterministic indicator for this dimension; absence is not a strength."
                            : "50-point neutral reference plus confidence-weighted strength/risk findings; inspect linked evidence and findings.",
                    trend(old == null ? null : old.score(), score)));
        }
        int measuredWeight = metrics.stream().filter(m -> m.score() != null).mapToInt(ProductScoreMetricView::weight).sum();
        int evidenceCoverage = analysis.assessment().evidenceCoverage();
        Integer overall = measuredWeight >= 50 && evidenceCoverage >= 50
                ? (int) Math.round(metrics.stream().filter(m -> m.score() != null).mapToDouble(m -> m.score() * m.weight()).sum() / measuredWeight)
                : null;
        double overallConfidence = measuredWeight == 0 ? 0 : round(metrics.stream().filter(m -> m.score() != null)
                .mapToDouble(m -> m.confidence() * m.weight()).sum() / measuredWeight);
        String status = overall == null ? "INSUFFICIENT_EVIDENCE" : measuredWeight == 100 ? "COMPLETE" : "PARTIAL";
        var result = new ProductScorecardView("scorecard-" + UUID.randomUUID(), productId, workspaceId,
                analysis.analysisId(), analysis.compositionVersion(), now, overall, overallConfidence,
                measuredWeight, evidenceCoverage, status, previous == null ? "" : previous.scorecardId(),
                trend(sameMeasuredDimensions(previous, metrics) ? previous.overallScore() : null, overall), List.copyOf(metrics),
                List.of("Scores are comparative decision aids, not compliance grades.",
                        "Unscored dimensions are excluded from the weighted aggregate; measured weight is shown.",
                        "Static discovery does not establish runtime, pipeline, semantic bounded-context, or architecture-drift health."));
        store.saveScorecard(workspaceId, productId, result);
        audit.append(new ArchitectureEventEnvelope(null, ArchitectureEventType.PRODUCT_SCORECARD_GENERATED,
                workspaceId, ArchitectureEventSource.PRODUCT_COMPOSITION_SERVICE, Actor.system("product-scorecard-service"),
                CausationId.newId("generate-product-scorecard"), new CorrelationId(analysis.correlationId()), null,
                AuditRelevance.REQUIRED, MutationTarget.NEITHER,
                java.util.Map.of("productId", productId, "scorecardId", result.scorecardId(), "analysisId", analysis.analysisId()), null));
        return result;
    }

    ProductScorecardView latest(String w, String p) {
        requireProduct(w, p);
        var value = latestOrNull(w, p);
        if (value == null) throw new NoSuchElementException("Scorecard not found for Product: " + p);
        return value;
    }

    ProductScorecardView get(String w, String p, String id) {
        return history(w, p).stream().filter(s -> s.scorecardId().equals(id)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("Scorecard not found: " + id));
    }

    List<ProductScorecardView> history(String w, String p) { requireProduct(w, p); return store.scorecards(w, p); }

    private ProductScorecardView latestOrNull(String w, String p) {
        var all = store.scorecards(w, p);
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }

    private void requireProduct(String w, String p) {
        if (store.find(w, p).isEmpty()) throw new NoSuchElementException("Product not found: " + p);
    }

    private static int score(List<ProductArchitectureFindingView> findings) {
        double value = 50;
        for (var finding : findings) {
            int magnitude = switch (finding.severity()) {
                case "CRITICAL" -> 40;
                case "HIGH" -> 30;
                case "MEDIUM" -> 20;
                case "LOW" -> 10;
                default -> 5;
            };
            value += (finding.polarity().equals("STRENGTH") ? 1 : -1) * magnitude * finding.confidenceScore();
        }
        return (int) Math.round(Math.max(0, Math.min(100, value)));
    }

    private static double confidence(ProductArchitectureAnalysisView analysis, List<ProductArchitectureFindingView> findings,
                                     List<ProductArchitectureRecommendationView> recommendations, Instant now) {
        double coverage = Math.max(0, Math.min(1, analysis.assessment().evidenceCoverage() / 100.0));
        double quality = findings.stream().mapToDouble(ProductArchitectureFindingView::confidenceScore).average().orElse(0);
        long ageDays = Math.max(0, Duration.between(analysis.completedAt(), now).toDays());
        double recency = ageDays <= 30 ? 1 : ageDays <= 90 ? .5 : 0;
        double corroboration = findings.stream().flatMap(f -> f.supportingEvidenceIds().stream()).distinct().count() >= 2 ? 1 : 0;
        double review = recommendations.stream().anyMatch(r -> VALIDATED.contains(r.status())) ? 1 : 0;
        return round(coverage * .35 + quality * .25 + recency * .15 + corroboration * .15 + review * .10);
    }

    private static String trend(Integer oldScore, Integer score) {
        if (oldScore == null || score == null) return "NO_COMPARABLE_SCORE";
        return score > oldScore ? "IMPROVED" : score < oldScore ? "DECLINED" : "UNCHANGED";
    }

    private static boolean sameMeasuredDimensions(ProductScorecardView previous, List<ProductScoreMetricView> metrics) {
        return previous != null && previous.metrics().stream().filter(m -> m.score() != null).map(ProductScoreMetricView::metric).toList()
                .equals(metrics.stream().filter(m -> m.score() != null).map(ProductScoreMetricView::metric).toList());
    }

    private static double round(double value) { return Math.round(value * 1000) / 1000.0; }
}
