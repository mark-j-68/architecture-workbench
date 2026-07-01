package com.architectureworkbench.decisionintelligence;

import com.architectureworkbench.intelligence.Concern;
import com.architectureworkbench.intelligence.DecisionOutcome;
import com.architectureworkbench.intelligence.DecisionService;
import com.architectureworkbench.intelligence.DecisionStatus;
import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.FindingService;
import com.architectureworkbench.intelligence.HumanOrAutomated;
import com.architectureworkbench.intelligence.Observation;
import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.intelligence.RecommendationService;
import com.architectureworkbench.intelligence.Reviewer;
import com.architectureworkbench.intelligence.ReviewerType;
import com.architectureworkbench.intelligence.Severity;
import com.architectureworkbench.intelligence.ConfidenceCalculator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionIntelligenceTest {
    @Test
    void supportsFullEvidenceToLearningTraceabilityChain() {
        AimFixture aim = new AimFixture();
        Hypothesis hypothesis = new HypothesisService().createHypothesis(
                "Adding ADRs for API boundaries will improve governance traceability.",
                "Discovery found API controllers without ADR evidence.",
                List.of(aim.evidence),
                List.of(aim.recommendation)
        );
        Hypothesis validated = new HypothesisService().validateHypothesis(hypothesis);
        DecisionOutcome decision = new DecisionService().recordDecision(
                DecisionStatus.ACCEPTED,
                "Run an ADR experiment for the API boundary.",
                List.of(aim.reviewer),
                List.of(aim.evidence),
                List.of(aim.recommendation)
        );
        Experiment experiment = new ExperimentService().recordExperiment(
                validated,
                Instant.now(),
                List.of("traceability improves"),
                List.of("traceability improves"),
                List.of()
        );
        Outcome outcome = new ExperimentService().compareExpectedVsActualOutcomes(experiment);
        Learning learning = new LearningService().deriveLearning("ADR-backed API boundaries improve governance traceability.", List.of(experiment), outcome);
        Pattern pattern = new LearningService().deriveReusableArchitecturalPattern(
                learning,
                "ADR-backed API Boundary",
                "Record an ADR for material API boundaries discovered or designed by the platform.",
                "Use when an API boundary is material to integration, security, or ownership.",
                List.of("Additional documentation effort")
        );
        Learning reusable = new LearningService().attachReusablePatterns(learning, List.of(pattern));

        assertEquals(HypothesisStatus.VALIDATED, validated.status());
        assertEquals(aim.recommendation, decision.recommendations().get(0));
        assertEquals(SuccessLevel.SUCCESS, outcome.successLevel());
        assertEquals(validated, experiment.hypothesis());
        assertEquals(experiment, learning.relatedExperiments().get(0));
        assertEquals(pattern, reusable.produceReusableArchitecturalPatterns().get(0));
        assertEquals(learning, pattern.supportingLearnings().get(0));
    }

    @Test
    void rejectsHypothesesWithoutEvidenceOrRecommendations() {
        AimFixture aim = new AimFixture();

        assertThrows(IllegalArgumentException.class, () -> new Hypothesis(null, "Statement", "Rationale", List.of(), List.of(aim.recommendation), 0.8, HypothesisStatus.PROPOSED));
        assertThrows(IllegalArgumentException.class, () -> new Hypothesis(null, "Statement", "Rationale", List.of(aim.evidence), List.of(), 0.8, HypothesisStatus.PROPOSED));
    }

    @Test
    void comparesExpectedAndActualOutcomes() {
        AimFixture aim = new AimFixture();
        Hypothesis hypothesis = new HypothesisService().createHypothesis("Use tests to reduce delivery risk.", "Finding shows missing tests.", List.of(aim.evidence), List.of(aim.recommendation));
        Experiment success = new ExperimentService().recordExperiment(hypothesis, Instant.now(), List.of("tests added"), List.of("tests added"), List.of());
        Experiment partial = new ExperimentService().recordExperiment(hypothesis, Instant.now(), List.of("tests added", "coverage improves"), List.of("tests added"), List.of());
        Experiment failure = new ExperimentService().recordExperiment(hypothesis, Instant.now(), List.of("tests added"), List.of("no tests added"), List.of());

        assertEquals(SuccessLevel.SUCCESS, new ExperimentService().compareExpectedVsActualOutcomes(success).successLevel());
        assertEquals(SuccessLevel.PARTIAL_SUCCESS, new ExperimentService().compareExpectedVsActualOutcomes(partial).successLevel());
        assertEquals(SuccessLevel.FAILURE, new ExperimentService().compareExpectedVsActualOutcomes(failure).successLevel());
    }

    @Test
    void learningCanProduceReusableArchitecturalPatterns() {
        AimFixture aim = new AimFixture();
        Hypothesis hypothesis = new HypothesisService().createHypothesis("Record evidence for critical findings.", "Evidence improves auditability.", List.of(aim.evidence), List.of(aim.recommendation));
        Experiment experiment = new ExperimentService().recordExperiment(hypothesis, Instant.now(), List.of("auditability improves"), List.of("auditability improves"), List.of());
        Outcome outcome = new ExperimentService().compareExpectedVsActualOutcomes(experiment);
        Learning learning = new LearningService().deriveLearning("Evidence-backed findings improve auditability.", List.of(experiment), outcome);
        Pattern pattern = new LearningService().deriveReusableArchitecturalPattern(learning, "Evidence-backed Finding", "Attach evidence to material findings.", "Regulated architecture review.", List.of("Evidence curation overhead"));
        Learning withPattern = new LearningService().attachReusablePatterns(learning, List.of(pattern));

        assertFalse(withPattern.produceReusableArchitecturalPatterns().isEmpty());
        assertEquals("Evidence-backed Finding", withPattern.produceReusableArchitecturalPatterns().get(0).name());
    }

    private static class AimFixture {
        final Evidence evidence = new Evidence(null, "discovery", "unit-test", 0.8, Instant.now(), List.of("repo"), List.of("controller"));
        final Observation observation = new Observation(null, "discovery", "Controller has no ADR.", List.of(evidence), List.of("component-api"));
        final Finding finding = new FindingService(new ConfidenceCalculator()).promoteObservationsIntoFinding(Severity.WARNING, "governance", "API boundary lacks ADR.", List.of(observation));
        final Concern concern = new Concern(null, "Architecture traceability", "Material decisions need evidence.", "governance");
        final Recommendation recommendation = new RecommendationService(new ConfidenceCalculator()).associateWithConcerns(
                new RecommendationService(new ConfidenceCalculator()).promoteFindingsIntoRecommendation(
                        "Create an ADR for the API boundary.",
                        "The finding requires a decision record.",
                        List.of(finding),
                        "Medium",
                        "Low"
                ),
                List.of(concern)
        );
        final Reviewer reviewer = new Reviewer(null, ReviewerType.GOVERNANCE, List.of("traceability"), Optional.empty(), "1.0", HumanOrAutomated.AUTOMATED);
    }
}
