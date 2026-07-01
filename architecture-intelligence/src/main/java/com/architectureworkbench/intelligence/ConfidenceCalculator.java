package com.architectureworkbench.intelligence;

import java.util.Collection;

public class ConfidenceCalculator {
    public double calculateEvidenceConfidence(Collection<Evidence> evidence) {
        return average(evidence.stream().mapToDouble(Evidence::confidence).toArray());
    }

    public double calculateObservationConfidence(Collection<Observation> observations) {
        return average(observations.stream()
                .mapToDouble(observation -> calculateEvidenceConfidence(observation.relatedEvidence()))
                .toArray());
    }

    public double calculateFindingConfidence(Collection<Finding> findings) {
        return average(findings.stream().mapToDouble(Finding::confidence).toArray());
    }

    public double calculateRecommendationConfidence(Collection<Finding> findings, Collection<Concern> concerns) {
        double findingConfidence = calculateFindingConfidence(findings);
        double concernFactor = concerns == null || concerns.isEmpty() ? 0.9 : 1.0;
        return round(findingConfidence * concernFactor);
    }

    private static double average(double[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("confidence inputs must not be empty.");
        }
        double total = 0.0;
        for (double value : values) {
            total += value;
        }
        return round(total / values.length);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
