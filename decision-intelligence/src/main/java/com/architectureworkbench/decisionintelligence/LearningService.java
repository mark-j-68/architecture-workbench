package com.architectureworkbench.decisionintelligence;

import java.util.List;

public class LearningService {
    public Learning deriveLearning(String summary, List<Experiment> experiments, Outcome outcome) {
        double hypothesisConfidence = experiments.stream().mapToDouble(experiment -> experiment.hypothesis().confidence()).average().orElse(0.0);
        double outcomeFactor = switch (outcome.successLevel()) {
            case SUCCESS -> 1.0;
            case PARTIAL_SUCCESS -> 0.75;
            case INCONCLUSIVE -> 0.5;
            case FAILURE -> 0.35;
        };
        return new Learning(null, summary, experiments, round(hypothesisConfidence * outcomeFactor), List.of());
    }

    public Pattern deriveReusableArchitecturalPattern(
            Learning learning,
            String name,
            String description,
            String applicability,
            List<String> knownTradeoffs
    ) {
        return new Pattern(null, name, description, applicability, knownTradeoffs, List.of(learning));
    }

    public Learning attachReusablePatterns(Learning learning, List<Pattern> patterns) {
        return new Learning(learning.id(), learning.summary(), learning.relatedExperiments(), learning.confidence(), patterns);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
