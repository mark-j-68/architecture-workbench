package com.architectureworkbench.core.agent;

public interface ReviewerClient {
    String reviewerId();
    String provider();
    String model();
    ReviewerAssessment assess(ReviewRequest request, ArchitectureContext context, String prompt);
}
