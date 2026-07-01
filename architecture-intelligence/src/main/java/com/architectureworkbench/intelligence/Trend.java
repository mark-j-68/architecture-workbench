package com.architectureworkbench.intelligence;

public record Trend(
        String id,
        String metric,
        TrendDirection direction,
        String period
) {
    public Trend {
        id = AimIds.id("trend", id);
        metric = AimIds.required(metric, "metric");
        direction = java.util.Objects.requireNonNullElse(direction, TrendDirection.UNKNOWN);
        period = AimIds.required(period, "period");
    }
}
