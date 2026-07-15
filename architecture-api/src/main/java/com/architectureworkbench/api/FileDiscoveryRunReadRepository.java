package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.DiscoveryDiagnosticView;
import com.architectureworkbench.api.ApiDtos.DiscoveryEvidenceView;
import com.architectureworkbench.api.ApiDtos.DiscoveryMetricView;
import com.architectureworkbench.api.ApiDtos.DiscoveryObservationView;
import com.architectureworkbench.api.ApiDtos.DiscoveryPluginExecution;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunDetails;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import com.architectureworkbench.workspace.FileWorkspaceIntegrityService;
import com.architectureworkbench.workspace.WorkspaceId;

class FileDiscoveryRunReadRepository implements DiscoveryRunReadRepository {
    private final Path root;
    private final ObjectMapper mapper;
    private final FileWorkspaceIntegrityService integrityService;
    FileDiscoveryRunReadRepository(Path root, ObjectMapper mapper) {
        this(root, mapper, new FileWorkspaceIntegrityService(root));
    }
    FileDiscoveryRunReadRepository(Path root, ObjectMapper mapper, FileWorkspaceIntegrityService integrityService) {
        this.root = root; this.mapper = mapper.copy().findAndRegisterModules(); this.integrityService = integrityService;
    }
    @Override public synchronized DiscoveryRunDetails save(DiscoveryRunDetails details) {
        Path directory = directory(details.summary().workspaceId(), details.summary().runId());
        write(directory.resolve("run.json"), details.summary());
        write(directory.resolve("plugins.json"), details.plugins());
        write(directory.resolve("evidence.json"), details.evidence());
        write(directory.resolve("observations.json"), details.observations());
        write(directory.resolve("metrics.json"), details.metrics());
        write(directory.resolve("diagnostics.json"), details.diagnostics());
        integrityService.refreshManifest(WorkspaceId.of(details.summary().workspaceId()));
        return details;
    }
    @Override public synchronized List<DiscoveryRunSummary> findAll(String workspaceId) {
        Path runs = root.resolve(workspaceId).resolve("discovery-runs"); if (!Files.isDirectory(runs)) return List.of();
        try (Stream<Path> paths = Files.list(runs)) {
            return paths.filter(Files::isDirectory).map(path -> read(path.resolve("run.json"), DiscoveryRunSummary.class))
                    .sorted(Comparator.comparing(DiscoveryRunSummary::startedAt).reversed()).toList();
        } catch (IOException exception) { throw new IllegalStateException("Unable to list discovery runs for " + workspaceId, exception); }
    }
    @Override public synchronized Optional<DiscoveryRunDetails> findById(String workspaceId, String runId) {
        Path directory = directory(workspaceId, runId); if (!Files.exists(directory.resolve("run.json"))) return Optional.empty();
        DiscoveryRunSummary summary = read(directory.resolve("run.json"), DiscoveryRunSummary.class);
        List<DiscoveryEvidenceView> evidence = read(directory.resolve("evidence.json"), new TypeReference<>() {});
        Map<String, Integer> distribution = new java.util.LinkedHashMap<>();
        evidence.forEach(item -> distribution.merge(item.confidence().band(), 1, Integer::sum));
        return Optional.of(new DiscoveryRunDetails(summary,
                read(directory.resolve("plugins.json"), new TypeReference<List<DiscoveryPluginExecution>>() {}), evidence,
                read(directory.resolve("observations.json"), new TypeReference<List<DiscoveryObservationView>>() {}),
                read(directory.resolve("metrics.json"), new TypeReference<List<DiscoveryMetricView>>() {}),
                read(directory.resolve("diagnostics.json"), new TypeReference<List<DiscoveryDiagnosticView>>() {}), Map.copyOf(distribution)));
    }
    private Path directory(String workspaceId, String runId) { return root.resolve(workspaceId).resolve("discovery-runs").resolve(runId); }
    private void write(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent()); Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), value);
            try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException exception) { throw new IllegalStateException("Unable to persist discovery run file " + path, exception); }
    }
    private <T> T read(Path path, Class<T> type) { try { return mapper.readValue(path.toFile(), type); } catch (IOException e) { throw new IllegalStateException("Unable to read " + path, e); } }
    private <T> T read(Path path, TypeReference<T> type) { try { return mapper.readValue(path.toFile(), type); } catch (IOException e) { throw new IllegalStateException("Unable to read " + path, e); } }
}
