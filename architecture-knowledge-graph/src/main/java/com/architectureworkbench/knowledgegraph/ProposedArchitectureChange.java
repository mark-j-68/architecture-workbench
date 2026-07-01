package com.architectureworkbench.knowledgegraph;

import com.architectureworkbench.audit.CorrelationId;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProposedArchitectureChange(
        ProposedChangeId id,
        ProposedChangeType type,
        ProposedChangeStatus status,
        ProposedGraphMutation mutation,
        String workspaceId,
        CorrelationId correlationId,
        String recommendationId,
        List<String> findingIds,
        List<String> evidenceIds,
        Instant createdAt,
        Instant decidedAt,
        String decisionRationale
) {
    public ProposedArchitectureChange {
        id = Objects.requireNonNullElseGet(id, ProposedChangeId::newId);
        type = Objects.requireNonNull(type, "type");
        status = Objects.requireNonNullElse(status, ProposedChangeStatus.PROPOSED);
        mutation = Objects.requireNonNull(mutation, "mutation");
        if (type != mutation.changeType()) {
            throw new IllegalArgumentException("Proposed change type must match mutation type.");
        }
        workspaceId = required(workspaceId, "workspaceId");
        correlationId = Objects.requireNonNull(correlationId, "correlationId");
        recommendationId = required(recommendationId, "recommendationId");
        findingIds = List.copyOf(requireNonEmpty(findingIds, "findingIds"));
        evidenceIds = List.copyOf(requireNonEmpty(evidenceIds, "evidenceIds"));
        createdAt = Objects.requireNonNullElseGet(createdAt, Instant::now);
        decisionRationale = Objects.requireNonNullElse(decisionRationale, "");
    }

    public ProposedArchitectureChange withStatus(ProposedChangeStatus nextStatus, String rationale) {
        return new ProposedArchitectureChange(
                id,
                type,
                nextStatus,
                mutation,
                workspaceId,
                correlationId,
                recommendationId,
                findingIds,
                evidenceIds,
                createdAt,
                Instant.now(),
                rationale
        );
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    private static <T> List<T> requireNonEmpty(List<T> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be empty.");
        }
        return values;
    }
}
