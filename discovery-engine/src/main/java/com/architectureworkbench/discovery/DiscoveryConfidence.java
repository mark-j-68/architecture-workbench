package com.architectureworkbench.discovery;

import java.util.Objects;

public record DiscoveryConfidence(double value, String rationale) {
    public DiscoveryConfidence {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("Discovery confidence must be between 0.0 and 1.0.");
        }
        rationale = Objects.requireNonNullElse(rationale, "");
    }

    public static DiscoveryConfidence observedFact(String rationale) {
        return new DiscoveryConfidence(1.0, rationale);
    }

    public static DiscoveryConfidence high(String rationale) {
        return new DiscoveryConfidence(0.9, rationale);
    }

    public static DiscoveryConfidence inferred(double value, String rationale) {
        return new DiscoveryConfidence(value, rationale);
    }
}
