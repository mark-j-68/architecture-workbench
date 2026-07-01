package com.architectureworkbench.discovery;

public interface DiscoveryConnector {
    boolean supports(DiscoverySource source);
    DiscoveryResult discover(DiscoveryContext context);
}
