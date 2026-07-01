package com.architectureworkbench.reviewboard;

import java.util.UUID;

public record ReviewBoardSessionId(String value) {
    public ReviewBoardSessionId {
        if (value == null || value.isBlank()) {
            value = "review-board-session-" + UUID.randomUUID();
        }
    }

    public static ReviewBoardSessionId newId() {
        return new ReviewBoardSessionId(null);
    }
}
