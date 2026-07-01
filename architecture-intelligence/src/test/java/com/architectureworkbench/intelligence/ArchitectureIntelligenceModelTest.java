package com.architectureworkbench.intelligence;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIntelligenceModelTest {
    @Test
    void enforcesEvidenceToDecisionTraceabilityChain() {
        Evidence evidence = evidence("repo-scan", 0.8);
        Observation observation = new Observation(null, "discovery", "Controller found without ADR evidence.", List.of(evidence), List.of("component-api"));
        Finding finding = new Finding(null, Severity.WARNING, "governance", "API layer lacks decision evidence.", List.of(observation), 0.8);
        Recommendation recommendation = new Recommendation(
                null,
                "Create ADR for API boundary.",
                "Material API boundaries should be decision-backed.",
                List.of(),
                List.of(finding),
                "Medium",
                "Low",
                0.8,
                LifecycleStatus.PROPOSED
        );
        Reviewer reviewer = new Reviewer(null, ReviewerType.GOVERNANCE, List.of("traceability"), Optional.empty(), "1.0", HumanOrAutomated.AUTOMATED);
        DecisionOutcome decision = new DecisionOutcome(null, DecisionStatus.ACCEPTED, "ADR required.", List.of(reviewer), List.of(evidence), List.of(recommendation), Instant.now());

        assertEquals(evidence, observation.relatedEvidence().get(0));
        assertEquals(observation, finding.supportingObservations().get(0));
        assertEquals(finding, recommendation.supportingFindings().get(0));
        assertEquals(recommendation, decision.recommendations().get(0));
        assertFalse(decision.evidence().isEmpty());
    }

    @Test
    void rejectsObjectsThatBreakRequiredTraceability() {
        Evidence evidence = evidence("repo-scan", 0.8);

        assertThrows(IllegalArgumentException.class, () -> new Observation(null, "discovery", "No evidence.", List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Finding(null, Severity.ERROR, "security", "No observations.", List.of(), 0.8));
        assertThrows(IllegalArgumentException.class, () -> new Recommendation(null, "Fix", "Because", List.of(), List.of(), "High", "Low", 0.8, LifecycleStatus.PROPOSED));
        Observation observation = new Observation(null, "discovery", "Observed.", List.of(evidence), List.of());
        Finding finding = new Finding(null, Severity.ERROR, "security", "Found.", List.of(observation), 0.8);
        Recommendation recommendation = new Recommendation(null, "Fix", "Because", List.of(), List.of(finding), "High", "Low", 0.8, LifecycleStatus.PROPOSED);
        assertThrows(IllegalArgumentException.class, () -> new DecisionOutcome(null, DecisionStatus.DEFERRED, "No evidence.", List.of(), List.of(), List.of(recommendation), Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> new DecisionOutcome(null, DecisionStatus.DEFERRED, "No recommendation.", List.of(), List.of(evidence), List.of(), Instant.now()));
    }

    @Test
    void servicesPromoteObservationsFindingsAndRecommendations() {
        ConfidenceCalculator calculator = new ConfidenceCalculator();
        FindingService findingService = new FindingService(calculator);
        RecommendationService recommendationService = new RecommendationService(calculator);
        Evidence first = evidence("repo-scan", 0.8);
        Evidence second = evidence("healthcheck", 0.6);
        Observation observation = new Observation(null, "discovery", "Tests are missing.", List.of(first, second), List.of("workspace-graph"));

        Finding finding = findingService.promoteObservationsIntoFinding(Severity.WARNING, "delivery", "Missing tests reduce delivery confidence.", List.of(observation));
        Recommendation recommendation = recommendationService.promoteFindingsIntoRecommendation(
                "Add test suite.",
                "Healthcheck detected no test directory.",
                List.of(finding),
                "High",
                "Medium"
        );

        assertEquals(0.7, finding.confidence());
        assertEquals(finding, recommendation.supportingFindings().get(0));
        assertEquals(0.63, recommendation.confidence());
    }

    @Test
    void attachesEvidenceAndAssociatesRecommendationsWithConcerns() {
        Evidence evidence = evidence("repo-scan", 0.75);
        Evidence supplemental = evidence("runtime-scan", 0.65);
        Observation observation = new Observation(null, "discovery", "Dockerfile present.", List.of(evidence), List.of());
        Observation enriched = new ObservationService().attachEvidence(observation, supplemental);
        Finding finding = new Finding(null, Severity.INFO, "deployment", "Container packaging exists.", List.of(enriched), 0.7);
        Recommendation recommendation = new RecommendationService(new ConfidenceCalculator()).promoteFindingsIntoRecommendation(
                "Review deployment healthchecks.",
                "Container packaging should be paired with runtime checks.",
                List.of(finding),
                "Medium",
                "Medium"
        );
        Concern concern = new Concern(null, "Runtime operability", "Deployment should be observable and recoverable.", "operations");

        Recommendation associated = new RecommendationService(new ConfidenceCalculator()).associateWithConcerns(recommendation, List.of(concern));

        assertEquals(2, enriched.relatedEvidence().size());
        assertTrue(associated.relatedConcerns().contains(concern));
        assertEquals(0.7, associated.confidence());
    }

    @Test
    void recordsDecisionWithReviewersEvidenceAndRecommendations() {
        Evidence evidence = evidence("review-board", 0.9);
        Observation observation = new Observation(null, "review", "Critical PII finding.", List.of(evidence), List.of());
        Finding finding = new Finding(null, Severity.CRITICAL, "regulatory", "PII controls incomplete.", List.of(observation), 0.9);
        Recommendation recommendation = new Recommendation(null, "Add cryptographic shredding.", "PII requires erasure support.", List.of(), List.of(finding), "High", "Medium", 0.9, LifecycleStatus.PROPOSED);
        Reviewer reviewer = new Reviewer("reviewer-reg", ReviewerType.REGULATORY, List.of("gdpr"), Optional.of("stub"), "1.0", HumanOrAutomated.AUTOMATED);

        DecisionOutcome decision = new DecisionService().recordDecision(
                DecisionStatus.DEFERRED,
                "Wait for key-destruction design.",
                List.of(reviewer),
                List.of(evidence),
                List.of(recommendation)
        );

        assertEquals(DecisionStatus.DEFERRED, decision.status());
        assertEquals(reviewer, decision.reviewers().get(0));
        assertEquals(recommendation, decision.recommendations().get(0));
    }

    @Test
    void metricAndTrendAreTraceableToEvidence() {
        Evidence evidence = evidence("healthcheck", 0.8);
        Trend trend = new Trend(null, "test-coverage", TrendDirection.DEGRADING, "P30D");
        Metric metric = new Metric(null, "test-coverage", 42.0, trend, List.of(evidence));

        assertEquals("test-coverage", metric.name());
        assertEquals(TrendDirection.DEGRADING, metric.trend().direction());
        assertEquals(evidence, metric.evidence().get(0));
    }

    private static Evidence evidence(String source, double confidence) {
        return new Evidence(null, source, "unit-test", confidence, Instant.now(), List.of("ref:" + source), List.of("artifact:" + source));
    }
}
