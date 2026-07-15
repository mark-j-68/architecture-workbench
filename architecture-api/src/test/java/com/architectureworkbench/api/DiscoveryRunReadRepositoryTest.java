package com.architectureworkbench.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.architectureworkbench.api.ApiDtos.DiscoveryConfidenceView;
import com.architectureworkbench.api.ApiDtos.DiscoveryDiagnosticView;
import com.architectureworkbench.api.ApiDtos.DiscoveryEvidenceView;
import com.architectureworkbench.api.ApiDtos.DiscoveryMetricView;
import com.architectureworkbench.api.ApiDtos.DiscoveryObservationView;
import com.architectureworkbench.api.ApiDtos.DiscoveryPluginExecution;
import com.architectureworkbench.api.ApiDtos.DiscoveryProvenanceView;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunDetails;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.architectureworkbench.workspace.FileWorkspaceIntegrityService;
import com.architectureworkbench.workspace.WorkspaceId;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiscoveryRunReadRepositoryTest {
    @TempDir Path root;

    @Test void reloadsSplitRunFilesAndIsolatesWorkspaces() {
        DiscoveryRunDetails details = details("workspace-one", "run-one");
        new FileDiscoveryRunReadRepository(root, new ObjectMapper()).save(details);
        FileDiscoveryRunReadRepository restarted = new FileDiscoveryRunReadRepository(root, new ObjectMapper());
        DiscoveryRunDetails reloaded = restarted.findById("workspace-one", "run-one").orElseThrow();
        assertEquals("evidence-one", reloaded.evidence().getFirst().evidenceId());
        assertEquals(1, restarted.findAll("workspace-one").size());
        assertTrue(restarted.findAll("workspace-two").isEmpty());
        assertTrue(restarted.findById("workspace-two", "run-one").isEmpty());
        Path directory = root.resolve("workspace-one/discovery-runs/run-one");
        for (String file : List.of("run.json", "plugins.json", "evidence.json", "observations.json", "metrics.json", "diagnostics.json"))
            assertTrue(Files.exists(directory.resolve(file)));
        assertTrue(new FileWorkspaceIntegrityService(root).verifyWorkspace(WorkspaceId.of("workspace-one")).valid());
    }

    private static DiscoveryRunDetails details(String workspace, String run) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        DiscoveryConfidenceView confidence = new DiscoveryConfidenceView(.9, 90, "HIGH", "explicit");
        DiscoveryEvidenceView evidence = new DiscoveryEvidenceView("evidence-one", "java-package", "Package", "sample",
                new DiscoveryProvenanceView("src/A.java", ".", "sample", "A", 1, "java.structure", "path:src/A.java"),
                confidence, "observed", List.of(), "parsed", "", Map.of());
        DiscoveryRunSummary summary = new DiscoveryRunSummary(run, workspace, "/repo", "COMPLETED", now, now, "correlation", "causation", 1, 1, 1, 1, 0, 0, false);
        return new DiscoveryRunDetails(summary,
                List.of(new DiscoveryPluginExecution("java.structure", "Java", "Language", "SUCCEEDED", now, now, 1, 1, 1, List.of(), List.of(), List.of(), false)),
                List.of(evidence), List.of(new DiscoveryObservationView("observation", "package", "Package sample exists.", "java.structure", ".", "deterministic", confidence, "parsed", List.of("evidence-one"))),
                List.of(new DiscoveryMetricView("metric", "package-count", 1, "count", "repository", ".", "analysis.dependency-metrics", "counted", confidence, List.of("evidence-one"))),
                List.<DiscoveryDiagnosticView>of(), Map.of("HIGH", 1));
    }
}
