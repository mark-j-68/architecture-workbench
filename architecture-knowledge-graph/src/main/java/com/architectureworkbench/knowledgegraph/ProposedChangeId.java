package com.architectureworkbench.knowledgegraph;

import java.util.Objects;
import java.util.UUID;

public record ProposedChangeId(String value) {
    public ProposedChangeId {
        if (value == null || value.isBlank()) {
            value = "proposed-change-" + UUID.randomUUID();
        }
    }

    public static ProposedChangeId newId() {
        return new ProposedChangeId(null);
    }

    @Override
    public String toString() {
        return value;
    }
}
