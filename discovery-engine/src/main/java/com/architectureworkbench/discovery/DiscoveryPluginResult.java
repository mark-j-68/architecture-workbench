package com.architectureworkbench.discovery;

import java.time.Duration;
import java.util.Objects;

public record DiscoveryPluginResult(
        DiscoveryPluginId pluginId,
        DiscoveryPluginStatus status,
        DiscoveryOutput output,
        Duration elapsed,
        String errorMessage
) {
    public DiscoveryPluginResult {
        pluginId = Objects.requireNonNull(pluginId, "pluginId");
        status = Objects.requireNonNull(status, "status");
        output = Objects.requireNonNullElseGet(output, DiscoveryOutput::empty);
        elapsed = Objects.requireNonNullElse(elapsed, Duration.ZERO);
        errorMessage = Objects.requireNonNullElse(errorMessage, "");
    }

    public static DiscoveryPluginResult succeeded(DiscoveryPluginId pluginId, DiscoveryOutput output, Duration elapsed) {
        return new DiscoveryPluginResult(pluginId, DiscoveryPluginStatus.SUCCEEDED, output, elapsed, "");
    }

    public static DiscoveryPluginResult failed(DiscoveryPluginId pluginId, Duration elapsed, String errorMessage) {
        return new DiscoveryPluginResult(pluginId, DiscoveryPluginStatus.FAILED, DiscoveryOutput.empty(), elapsed, errorMessage);
    }
}
