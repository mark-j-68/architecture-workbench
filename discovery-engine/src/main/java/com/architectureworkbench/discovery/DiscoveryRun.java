package com.architectureworkbench.discovery;

import com.architectureworkbench.intelligence.Evidence;
import com.architectureworkbench.intelligence.Finding;
import com.architectureworkbench.intelligence.Observation;
import com.architectureworkbench.intelligence.Recommendation;
import com.architectureworkbench.knowledgegraph.ProposedArchitectureChange;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record DiscoveryRun(
        DiscoveryRunId runId,
        DiscoverySource source,
        Instant startedAt,
        Instant completedAt,
        List<DiscoveredArtifact> artifacts,
        List<DiscoveryFinding> findings,
        List<Evidence> evidence,
        List<Observation> observations,
        List<Finding> aimFindings,
        List<Recommendation> recommendations,
        List<ProposedArchitectureChange> proposedChanges
) {
    public DiscoveryRun(
            DiscoveryRunId runId,
            DiscoverySource source,
            Instant startedAt,
            Instant completedAt,
            List<DiscoveredArtifact> artifacts,
            List<DiscoveryFinding> findings
    ) {
        this(runId, source, startedAt, completedAt, artifacts, findings, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public DiscoveryRun {
        runId = Objects.requireNonNull(runId, "runId");
        source = Objects.requireNonNull(source, "source");
        startedAt = Objects.requireNonNullElseGet(startedAt, Instant::now);
        completedAt = completedAt == null ? startedAt : completedAt;
        artifacts = List.copyOf(artifacts == null ? List.of() : artifacts);
        findings = List.copyOf(findings == null ? List.of() : findings);
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        observations = List.copyOf(observations == null ? List.of() : observations);
        aimFindings = List.copyOf(aimFindings == null ? List.of() : aimFindings);
        recommendations = List.copyOf(recommendations == null ? List.of() : recommendations);
        proposedChanges = List.copyOf(proposedChanges == null ? List.of() : proposedChanges);
    }
}
