package com.architectureworkbench.discovery;

import java.util.Objects;

public record DiscoveryPluginDependency(DiscoveryPluginId pluginId, boolean required) {
    public DiscoveryPluginDependency {
        pluginId = Objects.requireNonNull(pluginId, "pluginId");
    }
}
