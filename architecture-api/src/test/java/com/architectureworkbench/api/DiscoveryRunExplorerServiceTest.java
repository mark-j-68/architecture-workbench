package com.architectureworkbench.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.architectureworkbench.api.ApiDtos.DiscoveryRunDetails;
import com.architectureworkbench.audit.ArchitectureEventType;
import com.architectureworkbench.audit.InMemoryAuditSink;
import com.architectureworkbench.audit.MutationTarget;
import com.architectureworkbench.discovery.DiscoveryConfidence;
import com.architectureworkbench.discovery.DiscoveryEvidence;
import com.architectureworkbench.discovery.DiscoveryExecutionContext;
import com.architectureworkbench.discovery.DiscoveryInput;
import com.architectureworkbench.discovery.DiscoveryObservation;
import com.architectureworkbench.discovery.DiscoveryOutput;
import com.architectureworkbench.discovery.DiscoveryPlugin;
import com.architectureworkbench.discovery.DiscoveryPluginCapability;
import com.architectureworkbench.discovery.DiscoveryPluginId;
import com.architectureworkbench.discovery.DiscoveryPluginMetadata;
import com.architectureworkbench.discovery.DiscoveryPluginPipeline;
import com.architectureworkbench.discovery.DiscoveryPluginResult;
import com.architectureworkbench.discovery.DiscoveryPluginStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiscoveryRunExplorerServiceTest {
    @TempDir java.nio.file.Path root;

    @Test void isolatesFailedAndPartialPluginsAndEmitsTypedLifecycleMetadata() {
        DiscoveryPlugin success = plugin("test.success", (input, context) -> {
            DiscoveryEvidence evidence = new DiscoveryEvidence("evidence-source", "java-package", "test.success", "path:A.java", "sample",
                    DiscoveryConfidence.observedFact("explicit"), true, Instant.EPOCH, List.of("A.java"),
                    Map.of("filePath", "A.java", "module", ".", "packageName", "sample", "line", "1"));
            return DiscoveryPluginResult.succeeded(DiscoveryPluginId.of("test.success"), new DiscoveryOutput(List.of(evidence), List.of(), List.of()), Duration.ZERO);
        });
        DiscoveryPlugin failed = plugin("test.failed", (input, context) -> { throw new IllegalStateException("synthetic parser failure"); });
        DiscoveryPlugin partial = plugin("test.partial", (input, context) -> {
            DiscoveryObservation observation = new DiscoveryObservation("observation-one", "package-observed", "Package sample is present.",
                    DiscoveryConfidence.observedFact("joined"), List.of("evidence-source"), Instant.EPOCH);
            return new DiscoveryPluginResult(DiscoveryPluginId.of("test.partial"), DiscoveryPluginStatus.PARTIAL_SUCCESS,
                    new DiscoveryOutput(List.of(), List.of(observation), List.of("synthetic truncation warning")), Duration.ZERO, "bounded traversal");
        });
        InMemoryAuditSink audit = new InMemoryAuditSink();
        DiscoveryRunExplorerService service = new DiscoveryRunExplorerService(new DiscoveryPluginPipeline(List.of(success, failed, partial)),
                new InMemoryDiscoveryRunReadRepository(), audit);

        DiscoveryRunDetails run = service.run("workspace-one", root.toString(), "architect");
        assertEquals("PARTIALLY_COMPLETED", run.summary().status());
        assertEquals(1, run.summary().failureCount());
        assertTrue(run.summary().partialSuccess());
        assertEquals(1, run.evidence().size());
        assertEquals(List.of("evidence-source"), run.observations().getFirst().supportingEvidenceIds());
        assertTrue(run.diagnostics().stream().anyMatch(d -> d.severity().equals("ERROR") && d.message().contains("synthetic parser failure")));
        assertTrue(run.diagnostics().stream().anyMatch(d -> d.severity().equals("WARNING") && d.message().contains("truncation")));

        var lifecycle = audit.entries().stream().filter(event -> event.architectureEvent() != null).toList();
        assertEquals(2, lifecycle.size());
        assertEquals(ArchitectureEventType.DISCOVERY_STARTED, lifecycle.get(0).architectureEvent().eventType());
        assertEquals(ArchitectureEventType.DISCOVERY_COMPLETED, lifecycle.get(1).architectureEvent().eventType());
        assertEquals(lifecycle.get(0).architectureEvent().correlationId(), lifecycle.get(1).architectureEvent().correlationId());
        assertNotNull(lifecycle.get(0).architectureEvent().causationId());
        assertEquals(MutationTarget.NEITHER, lifecycle.get(1).architectureEvent().mutationTarget());
        assertFalse(run.plugins().get(2).errors().size() > 0);
    }

    private static DiscoveryPlugin plugin(String id, Runner runner) {
        return new DiscoveryPlugin() {
            @Override public DiscoveryPluginMetadata metadata() {
                return new DiscoveryPluginMetadata(DiscoveryPluginId.of(id), id, "0.2.6", "Test Plugin", List.of("test"),
                        List.of(DiscoveryPluginCapability.REGISTER_SOURCE), List.of(), true);
            }
            @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) { return runner.run(input, context); }
        };
    }
    @FunctionalInterface private interface Runner { DiscoveryPluginResult run(DiscoveryInput input, DiscoveryExecutionContext context); }
}
