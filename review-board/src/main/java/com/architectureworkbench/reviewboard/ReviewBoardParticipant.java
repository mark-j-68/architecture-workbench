package com.architectureworkbench.reviewboard;

import java.util.Objects;

public record ReviewBoardParticipant(
        String participantId,
        String name,
        ReviewBoardParticipantType participantType
) {
    public ReviewBoardParticipant {
        participantId = required(participantId, "participantId");
        name = required(name, "name");
        participantType = Objects.requireNonNull(participantType, "participantType");
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
