package com.architectureworkbench.reviewboard;

import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ReviewBoardSession(
        ReviewBoardSessionId sessionId,
        String workspaceId,
        CorrelationId correlationId,
        ReviewBoardSessionStatus status,
        List<Recommendation> recommendationCandidates,
        List<ProposedArchitectureChange> proposedChanges,
        List<ReviewBoardParticipant> participants,
        List<ReviewBoardVote> votes,
        ReviewBoardDecision decision,
        Instant openedAt,
        Instant closedAt
) {
    public ReviewBoardSession {
        sessionId = Objects.requireNonNullElseGet(sessionId, ReviewBoardSessionId::newId);
        workspaceId = required(workspaceId, "workspaceId");
        correlationId = Objects.requireNonNull(correlationId, "correlationId");
        status = Objects.requireNonNullElse(status, ReviewBoardSessionStatus.OPEN);
        recommendationCandidates = List.copyOf(requireAtLeastOne(recommendationCandidates, proposedChanges));
        proposedChanges = List.copyOf(proposedChanges == null ? List.of() : proposedChanges);
        participants = List.copyOf(participants == null ? List.of() : participants);
        votes = List.copyOf(votes == null ? List.of() : votes);
        openedAt = Objects.requireNonNullElseGet(openedAt, Instant::now);
    }

    private static List<Recommendation> requireAtLeastOne(List<Recommendation> recommendations, List<ProposedArchitectureChange> changes) {
        List<Recommendation> safeRecommendations = recommendations == null ? List.of() : recommendations;
        List<ProposedArchitectureChange> safeChanges = changes == null ? List.of() : changes;
        if (safeRecommendations.isEmpty() && safeChanges.isEmpty()) {
            throw new IllegalArgumentException("Review board session requires recommendations or proposed changes.");
        }
        return safeRecommendations;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
