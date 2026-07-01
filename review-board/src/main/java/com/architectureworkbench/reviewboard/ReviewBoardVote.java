package com.architectureworkbench.reviewboard;

import java.time.Instant;
import java.util.Objects;

public record ReviewBoardVote(
        String voteId,
        String participantId,
        ReviewBoardVoteType voteType,
        String rationale,
        Instant votedAt
) {
    public ReviewBoardVote {
        voteId = required(voteId == null || voteId.isBlank() ? "vote-" + java.util.UUID.randomUUID() : voteId, "voteId");
        participantId = required(participantId, "participantId");
        voteType = Objects.requireNonNull(voteType, "voteType");
        rationale = Objects.requireNonNullElse(rationale, "");
        votedAt = Objects.requireNonNullElseGet(votedAt, Instant::now);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
