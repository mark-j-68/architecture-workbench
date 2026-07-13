package com.architectureworkbench.discovery;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class DiscoveryIdentity {
    private DiscoveryIdentity() {
    }

    static String stableId(String prefix, String... parts) {
        String material = String.join("\u001f", parts);
        return prefix + "-" + UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8));
    }

    static String evidenceId(DiscoveryPluginId pluginId, String identity) {
        return stableId("discovery-evidence", pluginId.value(), identity);
    }

    static String observationId(DiscoveryPluginId pluginId, String identity) {
        return stableId("discovery-observation", pluginId.value(), identity);
    }
}
