package com.architectureworkbench.agentcollaboration;

public interface ArchitectureReviewer {
    ReviewerType reviewerType();
    String reviewerId();
    ReviewResponse review(ReviewRequest request);
}
