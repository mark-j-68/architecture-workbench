package com.architectureworkbench.audit;

import java.util.Map;

public record ReviewRequested(String reviewId, int reviewerCount, boolean externalProviderCalled) {
    public ReviewRequested {
        reviewId = AuditEventRecord.required(reviewId, "reviewId");
    }

    public Map<String, String> payload() {
        return Map.of(
                "reviewId", reviewId,
                "reviewerCount", String.valueOf(reviewerCount),
                "externalProviderCalled", String.valueOf(externalProviderCalled)
        );
    }
}
