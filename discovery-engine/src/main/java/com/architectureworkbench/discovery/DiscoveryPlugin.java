package com.architectureworkbench.discovery;

public interface DiscoveryPlugin {
    DiscoveryPluginMetadata metadata();

    DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context);
}
