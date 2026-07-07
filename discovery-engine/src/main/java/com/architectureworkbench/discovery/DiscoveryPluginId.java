package com.architectureworkbench.discovery;

public record DiscoveryPluginId(String value) {
    public DiscoveryPluginId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Discovery plugin id is required.");
        }
    }

    public static DiscoveryPluginId of(String value) {
        return new DiscoveryPluginId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
