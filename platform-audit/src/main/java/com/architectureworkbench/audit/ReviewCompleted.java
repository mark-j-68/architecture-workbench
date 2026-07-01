package com.architectureworkbench.audit;

import java.util.Map;

public record ReviewCompleted(String reviewId, int responseCount, String consensusAuditEventId, boolean externalProviderCalled) {
    public ReviewCompleted {
        reviewId = AuditEventRecord.required(reviewId, "reviewId");
        consensusAuditEventId = consensusAuditEventId == null ? "" : consensusAuditEventId;
    }

    public Map<String, String> payload() {
        return Map.of(
                "reviewId", reviewId,
                "responseCount", String.valueOf(responseCount),
                "consensusAuditEventId", consensusAuditEventId,
                "externalProviderCalled", String.valueOf(externalProviderCalled)
        );
    }
}
