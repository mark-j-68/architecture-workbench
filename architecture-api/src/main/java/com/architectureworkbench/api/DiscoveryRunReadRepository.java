package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.DiscoveryRunDetails;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunSummary;
import java.util.List;
import java.util.Optional;

interface DiscoveryRunReadRepository {
    DiscoveryRunDetails save(DiscoveryRunDetails details);
    List<DiscoveryRunSummary> findAll(String workspaceId);
    Optional<DiscoveryRunDetails> findById(String workspaceId, String runId);
}
