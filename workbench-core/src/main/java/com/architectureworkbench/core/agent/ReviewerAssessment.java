package com.architectureworkbench.core.agent;

import java.util.List;

public record ReviewerAssessment(
        String reviewerId,
        String provider,
        String model,
        String verdict,
        double confidence,
        List<String> strengths,
        List<String> risks,
        List<String> recommendations,
        String prompt,
        String response,
        String activityId
) {}
