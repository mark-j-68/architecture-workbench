package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.DiscoveryRunDetails;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunSummary;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class InMemoryDiscoveryRunReadRepository implements DiscoveryRunReadRepository {
    private final Map<String, DiscoveryRunDetails> runs = new ConcurrentHashMap<>();
    @Override public DiscoveryRunDetails save(DiscoveryRunDetails details) {
        runs.put(key(details.summary().workspaceId(), details.summary().runId()), details); return details;
    }
    @Override public List<DiscoveryRunSummary> findAll(String workspaceId) {
        return runs.values().stream().map(DiscoveryRunDetails::summary).filter(run -> run.workspaceId().equals(workspaceId))
                .sorted(Comparator.comparing(DiscoveryRunSummary::startedAt).reversed()).toList();
    }
    @Override public Optional<DiscoveryRunDetails> findById(String workspaceId, String runId) {
        return Optional.ofNullable(runs.get(key(workspaceId, runId)));
    }
    private static String key(String workspaceId, String runId) { return workspaceId + "/" + runId; }
}
