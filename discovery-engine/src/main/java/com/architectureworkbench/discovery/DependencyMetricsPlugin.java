package com.architectureworkbench.discovery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Emits traceable structural metrics; it deliberately does not compute an architecture score. */
public class DependencyMetricsPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("analysis.dependency-metrics");
    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Dependency Metrics Plugin", "0.2.5", "Structural Analysis Plugin",
                List.of("repository", "maven", "java", "spring", "openapi", "asyncapi", "messaging"),
                List.of(DiscoveryPluginCapability.CALCULATE_DEPENDENCY_METRICS),
                List.of(new DiscoveryPluginDependency(RepositoryDiscoveryPlugin.ID, true),
                        new DiscoveryPluginDependency(PackageCycleAnalysisPlugin.ID, false),
                        new DiscoveryPluginDependency(ComponentDependencyAnalysisPlugin.ID, false),
                        new DiscoveryPluginDependency(ContractVersionAnalysisPlugin.ID, false),
                        new DiscoveryPluginDependency(MessagingTopologyAnalysisPlugin.ID, false)), true);
    }
    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now(); List<DiscoveryEvidence> out = new ArrayList<>(); List<DiscoveryObservation> obs = new ArrayList<>(); List<String> diagnostics = new ArrayList<>();
        List<DiscoveryEvidence> prior = StructuralAnalysisSupport.prior(input); Map<String, List<DiscoveryEvidence>> byType = new HashMap<>();
        prior.forEach(e -> byType.computeIfAbsent(e.evidenceType(), ignored -> new ArrayList<>()).add(e));
        DiscoveryEvidence scope = StructuralAnalysisSupport.evidence(ID, input, "structural-metric-input-scope", "discovery-input",
                DiscoveryConfidence.observedFact("Defines the supplied evidence set over which metrics are calculated."), true,
                ids(prior), StructuralAnalysisSupport.details("inputEvidenceCount", Integer.toString(prior.size()),
                        "derivation", "All prior discovery and structural-analysis evidence supplied to the metric plugin."));
        out.add(scope);
        count(input, out, obs, "package-count", byType.getOrDefault("java-package", List.of()));
        count(input, out, obs, "class-count", types(byType, "java-class", "java-interface", "java-enum", "java-record", "java-annotation"));
        countDistinct(input, out, obs, "module-count", types(byType, "build-module", "build-module-declaration"), e -> first(e.attributes().get("module"), e.attributes().get("artifactId"), e.identity()));
        List<DiscoveryEvidence> packageEdges = byType.getOrDefault("package-dependency", List.of()); count(input, out, obs, "dependency-edge-count", packageEdges);
        count(input, out, obs, "cycle-count", types(byType, "direct-package-cycle", "multi-package-cycle"));
        packageFanMetrics(input, out, obs, packageEdges);
        count(input, out, obs, "component-dependency-count", byType.getOrDefault("component-dependency-path", List.of()));
        List<DiscoveryEvidence> contracts = types(byType, "api-contract", "openapi-document", "asyncapi-message", "event-contract", "java-event-contract", "command-contract", "java-command-contract");
        countDistinct(input, out, obs, "contract-count", contracts, e -> first(e.attributes().get("contractId"), e.identity()));
        List<DiscoveryEvidence> topology = types(byType, "topology-producer-channel", "topology-channel-consumer");
        countDistinct(input, out, obs, "channel-count", topology, e -> first(e.attributes().get("channelName"), e.attributes().get("from"), e.attributes().get("to")));
        countDistinct(input, out, obs, "producer-count", byType.getOrDefault("topology-producer-channel", List.of()), e -> e.attributes().getOrDefault("from", ""));
        countDistinct(input, out, obs, "consumer-count", byType.getOrDefault("topology-channel-consumer", List.of()), e -> e.attributes().getOrDefault("to", ""));
        versionCoverage(input, out, obs, contracts, byType.getOrDefault("contract-version", List.of()));
        presence(input, out, obs, "test-source-presence", prior.stream().filter(e -> e.evidenceType().equals("java-test-root")
                || e.attributes().getOrDefault("sourceKind", "").equals("test") || firstRef(e).contains("src/test")).toList());
        presence(input, out, obs, "adr-presence", prior.stream().filter(e -> firstRef(e).toLowerCase().contains("/adr/")
                || e.identity().toLowerCase().contains("adr")).toList());
        presence(input, out, obs, "documentation-presence", prior.stream().filter(e -> firstRef(e).toLowerCase().endsWith(".md")).toList());
        return StructuralAnalysisSupport.result(ID, started, out, obs, diagnostics, false);
    }
    private static void packageFanMetrics(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, List<DiscoveryEvidence> edges) {
        Map<String, Set<String>> incoming = new LinkedHashMap<>(), outgoing = new LinkedHashMap<>();
        for (DiscoveryEvidence e : edges) { String from = e.attributes().getOrDefault("sourcePackage", ""), to = e.attributes().getOrDefault("targetPackage", ""); if (from.isBlank() || to.isBlank()) continue;
            outgoing.computeIfAbsent(from, ignored -> new LinkedHashSet<>()).add(to); incoming.computeIfAbsent(to, ignored -> new LinkedHashSet<>()).add(from); incoming.putIfAbsent(from, new LinkedHashSet<>()); outgoing.putIfAbsent(to, new LinkedHashSet<>()); }
        int nodes = Math.max(incoming.size(), outgoing.size()); double avgIn = nodes == 0 ? 0 : incoming.values().stream().mapToInt(Set::size).average().orElse(0);
        double avgOut = nodes == 0 ? 0 : outgoing.values().stream().mapToInt(Set::size).average().orElse(0); double maxIn = incoming.values().stream().mapToInt(Set::size).max().orElse(0), maxOut = outgoing.values().stream().mapToInt(Set::size).max().orElse(0);
        metric(input, out, obs, new StructuralMetric("average-package-fan-in", avgIn, "edges-per-package", ids(edges), "Average distinct incoming package edges."));
        metric(input, out, obs, new StructuralMetric("average-package-fan-out", avgOut, "edges-per-package", ids(edges), "Average distinct outgoing package edges."));
        metric(input, out, obs, new StructuralMetric("maximum-package-fan-in", maxIn, "count", ids(edges), "Maximum distinct incoming package edges."));
        metric(input, out, obs, new StructuralMetric("maximum-package-fan-out", maxOut, "count", ids(edges), "Maximum distinct outgoing package edges."));
    }
    private static void versionCoverage(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, List<DiscoveryEvidence> contracts, List<DiscoveryEvidence> versions) {
        Set<String> contractIds = new LinkedHashSet<>(), versioned = new LinkedHashSet<>(); contracts.forEach(e -> contractIds.add(first(e.attributes().get("contractId"), e.identity())));
        versions.forEach(e -> versioned.add(first(e.attributes().get("contractId"), e.identity()))); contractIds.remove(""); versioned.remove("");
        double coverage = contractIds.isEmpty() ? 0 : 100.0 * versioned.stream().filter(contractIds::contains).count() / contractIds.size(); List<DiscoveryEvidence> supporting = new ArrayList<>(contracts); supporting.addAll(versions);
        metric(input, out, obs, new StructuralMetric("explicit-version-coverage-percentage", coverage, "percent", ids(supporting),
                "Contracts with explicit version evidence divided by discovered contracts multiplied by 100."));
    }
    private static void count(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, String name, List<DiscoveryEvidence> evidence) {
        metric(input, out, obs, new StructuralMetric(name, evidence.size(), "count", ids(evidence), "Counted supporting evidence items."));
    }
    private static void countDistinct(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, String name, List<DiscoveryEvidence> evidence, java.util.function.Function<DiscoveryEvidence, String> key) {
        long count = evidence.stream().map(key).filter(v -> v != null && !v.isBlank()).distinct().count(); metric(input, out, obs, new StructuralMetric(name, count, "count", ids(evidence), "Counted distinct identities in supporting evidence."));
    }
    private static void presence(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, String name, List<DiscoveryEvidence> evidence) {
        metric(input, out, obs, new StructuralMetric(name, evidence.isEmpty() ? 0 : 1, "boolean", ids(evidence), "1 means supporting repository evidence is present; 0 means none was supplied."));
    }
    private static void metric(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, StructuralMetric metric) {
        List<String> supporting = metric.evidenceIds().isEmpty() && !out.isEmpty()
                ? List.of(out.getFirst().evidenceId()) : metric.evidenceIds();
        DiscoveryEvidence e = StructuralAnalysisSupport.evidence(ID, input, "structural-metric", metric.name(), DiscoveryConfidence.observedFact(metric.derivation()), true,
                supporting, StructuralAnalysisSupport.details("metricName", metric.name(), "value", Double.toString(metric.value()), "unit", metric.unit(), "derivation", metric.derivation()));
        out.add(e); obs.add(StructuralAnalysisSupport.observation(ID, "structural-metric-calculated", metric.name() + " = " + metric.value() + " " + metric.unit() + ".", e));
    }
    private static List<DiscoveryEvidence> types(Map<String, List<DiscoveryEvidence>> byType, String... types) { List<DiscoveryEvidence> r = new ArrayList<>(); for (String type : types) r.addAll(byType.getOrDefault(type, List.of())); return r; }
    private static List<String> ids(List<DiscoveryEvidence> evidence) { return evidence.stream().map(DiscoveryEvidence::evidenceId).distinct().sorted().toList(); }
    private static String firstRef(DiscoveryEvidence e) { return e.references().isEmpty() ? "" : e.references().get(0); }
    private static String first(String... values) { for (String v : values) if (v != null && !v.isBlank()) return v; return ""; }
}
