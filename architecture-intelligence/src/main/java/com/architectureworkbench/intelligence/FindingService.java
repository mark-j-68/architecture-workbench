package com.architectureworkbench.intelligence;

import java.util.List;

public class FindingService {
    private final ConfidenceCalculator confidenceCalculator;

    public FindingService(ConfidenceCalculator confidenceCalculator) {
        this.confidenceCalculator = confidenceCalculator;
    }

    public Finding promoteObservationsIntoFinding(
            Severity severity,
            String category,
            String description,
            List<Observation> observations
    ) {
        double confidence = confidenceCalculator.calculateObservationConfidence(observations);
        return new Finding(null, severity, category, description, observations, confidence);
    }
}
