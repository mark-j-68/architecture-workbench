package com.architectureworkbench.decisionintelligence;

import com.architectureworkbench.intelligence.Metric;
import java.time.Instant;
import java.util.List;

public class ExperimentService {
    public Experiment recordExperiment(
            Hypothesis hypothesis,
            Instant implementationDate,
            List<String> expectedOutcomes,
            List<String> actualOutcomes,
            List<Metric> metrics
    ) {
        return new Experiment(null, hypothesis, implementationDate, expectedOutcomes, actualOutcomes, metrics);
    }

    public Outcome compareExpectedVsActualOutcomes(Experiment experiment) {
        long matched = experiment.expectedOutcomes().stream()
                .filter(expected -> experiment.actualOutcomes().stream()
                        .anyMatch(actual -> normalize(actual).equals(normalize(expected))))
                .count();
        SuccessLevel successLevel;
        if (experiment.actualOutcomes().isEmpty()) {
            successLevel = SuccessLevel.INCONCLUSIVE;
        } else if (matched == experiment.expectedOutcomes().size()) {
            successLevel = SuccessLevel.SUCCESS;
        } else if (matched > 0) {
            successLevel = SuccessLevel.PARTIAL_SUCCESS;
        } else {
            successLevel = SuccessLevel.FAILURE;
        }
        var observations = experiment.hypothesis().relatedRecommendations().stream()
                .flatMap(recommendation -> recommendation.supportingFindings().stream())
                .flatMap(finding -> finding.supportingObservations().stream())
                .distinct()
                .toList();
        return new Outcome(
                null,
                successLevel,
                observations,
                experiment.hypothesis().supportingEvidence()
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
