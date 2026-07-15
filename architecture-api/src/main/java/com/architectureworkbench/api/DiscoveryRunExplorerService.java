package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.DiscoveryConfidenceView;
import com.architectureworkbench.api.ApiDtos.DiscoveryDiagnosticView;
import com.architectureworkbench.api.ApiDtos.DiscoveryEvidenceView;
import com.architectureworkbench.api.ApiDtos.DiscoveryMetricView;
import com.architectureworkbench.api.ApiDtos.DiscoveryObservationView;
import com.architectureworkbench.api.ApiDtos.DiscoveryPluginExecution;
import com.architectureworkbench.api.ApiDtos.DiscoveryProvenanceView;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunDetails;
import com.architectureworkbench.api.ApiDtos.DiscoveryRunSummary;
import com.architectureworkbench.audit.Actor;
import com.architectureworkbench.audit.ArchitectureEventEnvelope;
import com.architectureworkbench.audit.ArchitectureEventSource;
import com.architectureworkbench.audit.ArchitectureEventType;
import com.architectureworkbench.audit.AuditRelevance;
import com.architectureworkbench.audit.AuditSink;
import com.architectureworkbench.audit.CausationId;
import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.audit.DiscoveryCompleted;
import com.architectureworkbench.audit.DiscoveryStarted;
import com.architectureworkbench.audit.MutationTarget;
import com.architectureworkbench.discovery.DiscoveryEvidence;
import com.architectureworkbench.discovery.DiscoveryExecutionContext;
import com.architectureworkbench.discovery.DiscoveryObservation;
import com.architectureworkbench.discovery.DiscoveryPluginExecutionRecord;
import com.architectureworkbench.discovery.DiscoveryPluginPipeline;
import com.architectureworkbench.discovery.DiscoveryPluginStatus;
import com.architectureworkbench.discovery.DiscoveryRunId;
import com.architectureworkbench.discovery.DiscoverySource;
import com.architectureworkbench.discovery.DiscoverySourceType;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

class DiscoveryRunExplorerService {
    private final DiscoveryPluginPipeline pipeline;
    private final DiscoveryRunReadRepository repository;
    private final AuditSink auditSink;

    DiscoveryRunExplorerService(DiscoveryPluginPipeline pipeline, DiscoveryRunReadRepository repository, AuditSink auditSink) {
        this.pipeline = pipeline; this.repository = repository; this.auditSink = auditSink;
    }

    DiscoveryRunDetails run(String workspaceId, String sourceReference, String actorRef) {
        DiscoveryRunId runId = DiscoveryRunId.newId(); Instant startedAt = Instant.now();
        CorrelationId correlationId = CorrelationId.newId("discovery"); CausationId causationId = CausationId.newId("discovery-request");
        DiscoverySource source = new DiscoverySource("local-" + runId.value(), DiscoverySourceType.LOCAL_REPOSITORY,
                required(sourceReference, "path"), "Local repository");
        appendStarted(workspaceId, actorRef, correlationId, causationId, runId, source);
        DiscoveryExecutionContext context = new DiscoveryExecutionContext(runId, source, Path.of(sourceReference), actorRef, correlationId.value());
        List<DiscoveryPluginExecutionRecord> executions = pipeline.execute(context); Instant completedAt = Instant.now();
        DiscoveryRunDetails details = map(runId.value(), workspaceId, sourceReference, correlationId.value(), causationId.value(), startedAt, completedAt, executions);
        repository.save(details); appendCompleted(workspaceId, actorRef, correlationId, runId, details);
        return details;
    }

    List<DiscoveryRunSummary> list(String workspaceId) { return repository.findAll(workspaceId); }
    DiscoveryRunDetails details(String workspaceId, String runId) { return repository.findById(workspaceId, runId)
            .orElseThrow(() -> new IllegalArgumentException("Discovery run does not exist in workspace: " + runId)); }

    List<DiscoveryEvidenceView> evidence(String workspaceId, String runId, Map<String, String> filters) {
        double minimum = decimal(filters.get("minimumConfidence"), 0);
        return details(workspaceId, runId).evidence().stream()
                .filter(e -> matches(e.provenance().source(), filters.get("pluginId")))
                .filter(e -> matches(e.type(), filters.get("evidenceType")))
                .filter(e -> matches(e.provenance().module(), filters.get("module")))
                .filter(e -> matches(e.provenance().packageName(), filters.get("package")))
                .filter(e -> contains(e.provenance().repositoryRelativeFilePath(), filters.get("filePath")))
                .filter(e -> matches(e.classification(), filters.get("classification")))
                .filter(e -> e.confidence().value() >= minimum).toList();
    }
    List<DiscoveryObservationView> observations(String workspaceId, String runId, Map<String, String> filters) {
        double minimum = decimal(first(filters.get("minimumConfidence"), filters.get("confidence")), 0); String evidenceId = filters.get("supportingEvidenceId");
        return details(workspaceId, runId).observations().stream()
                .filter(o -> matches(o.pluginId(), filters.get("pluginId")))
                .filter(o -> matches(o.observationType(), first(filters.get("observationType"), filters.get("category"))))
                .filter(o -> matches(o.module(), filters.get("module")))
                .filter(o -> o.confidence().value() >= minimum)
                .filter(o -> evidenceId == null || evidenceId.isBlank() || o.supportingEvidenceIds().contains(evidenceId)).toList();
    }
    List<DiscoveryMetricView> metrics(String workspaceId, String runId, Map<String, String> filters) {
        return details(workspaceId, runId).metrics().stream().filter(m -> matches(m.name(), filters.get("metricName")))
                .filter(m -> matches(m.scope(), filters.get("scope"))).filter(m -> matches(m.module(), filters.get("module"))).toList();
    }
    List<DiscoveryDiagnosticView> diagnostics(String workspaceId, String runId, Map<String, String> filters) {
        return details(workspaceId, runId).diagnostics().stream().filter(d -> matches(d.pluginId(), filters.get("pluginId")))
                .filter(d -> matches(d.severity(), filters.get("severity"))).toList();
    }

    private DiscoveryRunDetails map(String runId, String workspaceId, String source, String correlationId, String causationId,
                                    Instant startedAt, Instant completedAt, List<DiscoveryPluginExecutionRecord> executions) {
        List<DiscoveryEvidence> rawEvidence = executions.stream().flatMap(e -> e.result().output().evidence().stream())
                .sorted(Comparator.comparing(DiscoveryEvidence::evidenceId)).toList();
        Map<String, DiscoveryEvidence> evidenceById = new LinkedHashMap<>(); rawEvidence.forEach(e -> evidenceById.put(e.evidenceId(), e));
        List<DiscoveryEvidenceView> evidence = rawEvidence.stream().map(this::evidenceView).toList();
        List<DiscoveryObservationView> observations = executions.stream().flatMap(execution -> execution.result().output().observations().stream()
                .map(observation -> observationView(execution.metadata().id().value(), observation, evidenceById))).toList();
        List<DiscoveryMetricView> metrics = rawEvidence.stream().filter(item -> item.evidenceType().equals("structural-metric"))
                .map(this::metricView).toList();
        List<DiscoveryPluginExecution> plugins = executions.stream().map(this::pluginView).toList();
        List<DiscoveryDiagnosticView> diagnostics = diagnostics(executions);
        int failures = (int) executions.stream().filter(e -> e.result().status() == DiscoveryPluginStatus.FAILED).count();
        boolean partial = failures > 0 || executions.stream().anyMatch(e -> e.result().status() == DiscoveryPluginStatus.PARTIAL_SUCCESS
                || e.result().status() == DiscoveryPluginStatus.SKIPPED);
        DiscoveryRunStatus status = failures == executions.size() ? DiscoveryRunStatus.FAILED
                : partial ? DiscoveryRunStatus.PARTIALLY_COMPLETED : DiscoveryRunStatus.COMPLETED;
        int warnings = (int) diagnostics.stream().filter(d -> d.severity().equals("WARNING")).count();
        DiscoveryRunSummary summary = new DiscoveryRunSummary(runId, workspaceId, source, status.name(), startedAt, completedAt,
                correlationId, causationId, plugins.size(), evidence.size(), observations.size(), metrics.size(), warnings, failures, partial);
        Map<String, Integer> distribution = new LinkedHashMap<>(); evidence.forEach(item -> distribution.merge(item.confidence().band(), 1, Integer::sum));
        return new DiscoveryRunDetails(summary, plugins, evidence, observations, metrics, diagnostics, Map.copyOf(distribution));
    }

    private DiscoveryPluginExecution pluginView(DiscoveryPluginExecutionRecord execution) {
        List<String> warnings = execution.result().status() == DiscoveryPluginStatus.FAILED ? List.of() : execution.result().output().diagnostics();
        List<String> errors = execution.result().status() == DiscoveryPluginStatus.FAILED ? List.of(execution.result().errorMessage()) : List.of();
        int metricCount = (int) execution.result().output().evidence().stream().filter(e -> e.evidenceType().equals("structural-metric")).count();
        return new DiscoveryPluginExecution(execution.metadata().id().value(), execution.metadata().name(), execution.metadata().category(),
                execution.result().status().name(), execution.startedAt(), execution.completedAt(), execution.result().output().evidence().size(),
                execution.result().output().observations().size(), metricCount, warnings, errors,
                execution.metadata().dependencies().stream().map(d -> d.pluginId().value() + (d.required() ? " (required)" : " (optional)")).toList(),
                execution.result().status() == DiscoveryPluginStatus.PARTIAL_SUCCESS);
    }

    private DiscoveryEvidenceView evidenceView(DiscoveryEvidence item) {
        Map<String, String> a = item.attributes(); String file = first(a.get("filePath"), item.references().stream().filter(r -> r.contains("/") || r.contains(".")).findFirst().orElse(""));
        DiscoveryProvenanceView provenance = new DiscoveryProvenanceView(file, a.getOrDefault("module", "."), a.getOrDefault("packageName", ""),
                first(a.get("symbol"), a.get("className"), item.identity()), integer(a.get("line")), item.source(), item.provenance());
        return new DiscoveryEvidenceView(item.evidenceId(), item.evidenceType(), title(item), item.identity(), provenance,
                confidence(item.confidence().value(), item.confidence().rationale()), item.directlyObserved() ? "observed" : "inferred",
                sourceIds(item), first(a.get("derivation"), a.get("explanation"), item.confidence().rationale()),
                first(a.get("uncertainty"), a.get("unresolvedValue")), a);
    }

    private DiscoveryObservationView observationView(String fallbackPluginId, DiscoveryObservation item, Map<String, DiscoveryEvidence> evidenceById) {
        DiscoveryEvidence source = item.relatedEvidenceIds().stream().map(evidenceById::get).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        String plugin = source == null ? fallbackPluginId : source.source(); String module = source == null ? "." : source.attributes().getOrDefault("module", ".");
        String classification = source != null && (source.confidence().value() < .9
                || !source.attributes().getOrDefault("uncertainty", "").isBlank()
                || source.evidenceType().equals("candidate-layer")) ? "heuristic" : "deterministic";
        String derivation = source == null ? item.confidence().rationale() : first(source.attributes().get("derivation"), source.attributes().get("explanation"), item.confidence().rationale());
        return new DiscoveryObservationView(item.observationId(), item.observationType(), item.description(), plugin, module, classification,
                confidence(item.confidence().value(), item.confidence().rationale()), derivation, item.relatedEvidenceIds());
    }

    private DiscoveryMetricView metricView(DiscoveryEvidence item) {
        Map<String, String> a = item.attributes(); return new DiscoveryMetricView(item.evidenceId(), a.getOrDefault("metricName", item.identity()),
                decimal(a.get("value"), 0), a.getOrDefault("unit", "count"), a.getOrDefault("scope", "repository"), a.getOrDefault("module", "."),
                item.source(), first(a.get("derivation"), item.confidence().rationale()), confidence(item.confidence().value(), item.confidence().rationale()), sourceIds(item));
    }

    private List<DiscoveryDiagnosticView> diagnostics(List<DiscoveryPluginExecutionRecord> executions) {
        List<DiscoveryDiagnosticView> result = new ArrayList<>(); int index = 0;
        for (DiscoveryPluginExecutionRecord execution : executions) {
            for (String warning : execution.result().output().diagnostics()) result.add(new DiscoveryDiagnosticView("diagnostic-" + index++,
                    execution.metadata().id().value(), "WARNING", warning, execution.completedAt()));
            for (DiscoveryEvidence item : execution.result().output().evidence()) {
                String uncertainty = first(item.attributes().get("uncertainty"), item.attributes().get("unresolvedValue"));
                if (!uncertainty.isBlank()) result.add(new DiscoveryDiagnosticView("diagnostic-" + index++,
                        execution.metadata().id().value(), "WARNING",
                        "Unresolved or dynamic value for " + item.identity() + ": " + uncertainty, execution.completedAt()));
            }
            if (execution.result().status() == DiscoveryPluginStatus.FAILED) result.add(new DiscoveryDiagnosticView("diagnostic-" + index++,
                    execution.metadata().id().value(), "ERROR", execution.result().errorMessage(), execution.completedAt()));
        }
        return List.copyOf(result);
    }

    private static List<String> sourceIds(DiscoveryEvidence item) {
        Set<String> ids = new LinkedHashSet<>(); Map<String, String> a = item.attributes();
        for (String key : List.of("sourceEvidenceIds", "supportingEvidenceIds", "supportingEvidenceId", "dependencyEvidenceId")) {
            String value = a.getOrDefault(key, ""); for (String id : value.split(",")) if (!id.isBlank()) ids.add(id.trim());
        }
        if (item.source().startsWith("analysis.")) ids.addAll(item.references());
        return List.copyOf(ids);
    }

    private static DiscoveryConfidenceView confidence(double value, String rationale) {
        String band = value >= .9 ? "HIGH" : value >= .7 ? "MEDIUM" : "LOW";
        return new DiscoveryConfidenceView(value, (int) Math.round(value * 100), band, rationale);
    }
    private static String title(DiscoveryEvidence item) { return item.evidenceType().replace('-', ' ') + ": " + item.identity(); }
    private static boolean matches(String actual, String expected) { return expected == null || expected.isBlank() || actual.equalsIgnoreCase(expected); }
    private static boolean contains(String actual, String expected) { return expected == null || expected.isBlank() || actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT)); }
    private static double decimal(String value, double fallback) { try { return value == null ? fallback : Double.parseDouble(value); } catch (NumberFormatException ignored) { return fallback; } }
    private static Integer integer(String value) { try { return value == null || value.isBlank() ? null : Integer.valueOf(value); } catch (NumberFormatException ignored) { return null; } }
    private static String first(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value; return ""; }
    private static String required(String value, String field) { if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required."); return value; }

    private void appendStarted(String workspaceId, String actorRef, CorrelationId correlation, CausationId causation, DiscoveryRunId runId, DiscoverySource source) {
        DiscoveryStarted event = new DiscoveryStarted(runId.value(), source.type().name(), source.uri());
        auditSink.append(new ArchitectureEventEnvelope(null, ArchitectureEventType.DISCOVERY_STARTED, workspaceId, ArchitectureEventSource.DISCOVERY_SERVICE,
                Actor.human(actorRef), causation, correlation, null, AuditRelevance.REQUIRED, MutationTarget.NEITHER, event.payload(), null));
    }
    private void appendCompleted(String workspaceId, String actorRef, CorrelationId correlation, DiscoveryRunId runId, DiscoveryRunDetails details) {
        DiscoveryCompleted event = new DiscoveryCompleted(runId.value(), details.summary().evidenceCount(), details.summary().failureCount());
        auditSink.append(new ArchitectureEventEnvelope(null, ArchitectureEventType.DISCOVERY_COMPLETED, workspaceId, ArchitectureEventSource.DISCOVERY_SERVICE,
                Actor.human(actorRef), CausationId.newId("discovery-completion"), correlation, null, AuditRelevance.REQUIRED, MutationTarget.NEITHER, event.payload(), null));
    }
}
