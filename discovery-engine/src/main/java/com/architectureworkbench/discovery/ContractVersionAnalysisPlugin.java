package com.architectureworkbench.discovery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compares literal producer/consumer contract versions and reports absence or uncertainty as observations. */
public class ContractVersionAnalysisPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("analysis.contract-version");
    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Contract Version Analysis Plugin", "0.2.5", "Structural Analysis Plugin",
                List.of("openapi", "asyncapi", "json-schema", "avro", "java"), List.of(DiscoveryPluginCapability.ANALYZE_CONTRACT_VERSIONS),
                List.of(new DiscoveryPluginDependency(ContractVersionDiscoveryPlugin.ID, true),
                        new DiscoveryPluginDependency(MessagingTopologyDiscoveryPlugin.ID, false)), true);
    }
    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now(); List<DiscoveryEvidence> out = new ArrayList<>(); List<DiscoveryObservation> obs = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>(); Map<String, ContractState> states = new LinkedHashMap<>();
        for (DiscoveryEvidence e : StructuralAnalysisSupport.prior(input)) {
            String contract = contractId(e); if (contract.isBlank()) continue;
            ContractState state = states.computeIfAbsent(contract, ContractState::new); String version = version(e);
            if (e.evidenceType().equals("contract-version")) { state.definitions.add(e); if (!version.isBlank()) state.versions.add(version); }
            if (e.evidenceType().equals("contract-compatibility") && Boolean.parseBoolean(e.attributes().getOrDefault("deprecated", "false"))) state.deprecated.add(e);
            if (isProducer(e)) state.producers.add(new VersionRef(e, version));
            if (isConsumer(e)) state.consumers.add(new VersionRef(e, version));
            if (isContract(e) && state.contractEvidence.stream().noneMatch(x -> x.evidenceId().equals(e.evidenceId()))) state.contractEvidence.add(e);
        }
        for (ContractState state : states.values().stream().sorted(Comparator.comparing(s -> s.id)).toList()) {
            List<String> definitionIds = state.allIds();
            if (state.versions.isEmpty()) add(input, out, obs, "contract-version-not-detected", state.id,
                    state.id + " has no explicit version detected.", DiscoveryConfidence.high("No explicit version exists in the supplied contract evidence; incomplete evidence remains possible."), false, definitionIds,
                    StructuralAnalysisSupport.details("contractId", state.id, "comparison", "UNRESOLVED", "reason", "missing-version-evidence"));
            else {
                add(input, out, obs, "contract-explicit-version-present", state.id + ":" + String.join(",", state.versions),
                        state.id + " has explicit version " + String.join(", ", state.versions) + ".", DiscoveryConfidence.observedFact("Version literals are explicit."), true, definitionIds,
                        StructuralAnalysisSupport.details("contractId", state.id, "versions", String.join(",", state.versions)));
                if (state.versions.size() > 1) add(input, out, obs, "contract-multiple-versions-detected", state.id,
                        state.id + " has multiple detected versions: " + String.join(", ", state.versions) + ".", DiscoveryConfidence.observedFact("Multiple distinct explicit literals were collected."), true, definitionIds,
                        StructuralAnalysisSupport.details("contractId", state.id, "versions", String.join(",", state.versions)));
            }
            if (!state.deprecated.isEmpty()) add(input, out, obs, "deprecated-contract-version-present", state.id,
                    "Deprecated version metadata is present for " + state.id + ".", DiscoveryConfidence.observedFact("Deprecation metadata is explicit."), true,
                    state.deprecated.stream().map(DiscoveryEvidence::evidenceId).toList(), StructuralAnalysisSupport.details("contractId", state.id));
            for (VersionRef producer : state.producers) reference(input, state.id, producer, true, out, obs);
            for (VersionRef consumer : state.consumers) reference(input, state.id, consumer, false, out, obs);
            for (VersionRef producer : state.producers) for (VersionRef consumer : state.consumers) compare(input, state.id, producer, consumer, out, obs);
        }
        return StructuralAnalysisSupport.result(ID, started, out, obs, diagnostics, false);
    }
    private static void reference(DiscoveryInput input, String contract, VersionRef reference, boolean producer,
                                  List<DiscoveryEvidence> out, List<DiscoveryObservation> obs) {
        String role = producer ? "producer" : "consumer";
        boolean resolved = !reference.version.isBlank() && !dynamic(reference.version);
        String description = resolved
                ? capitalize(role) + " " + reference.evidence.identity() + " references " + contract + " version " + reference.version + "."
                : capitalize(role) + " " + reference.evidence.identity() + " has no resolved literal version for " + contract + ".";
        add(input, out, obs, "contract-" + role + "-version-reference", reference.evidence.evidenceId(), description,
                resolved ? DiscoveryConfidence.observedFact("The version reference is an explicit literal.")
                        : DiscoveryConfidence.inferred(0.7, "The version reference is absent or dynamic."), resolved,
                List.of(reference.evidence.evidenceId()), StructuralAnalysisSupport.details("contractId", contract, "role", role,
                        "referencedVersion", reference.version, "comparison", resolved ? "LITERAL" : "UNRESOLVED"));
    }
    private static void compare(DiscoveryInput input, String contract, VersionRef producer, VersionRef consumer,
                                List<DiscoveryEvidence> out, List<DiscoveryObservation> obs) {
        VersionComparison.Result result = producer.version.isBlank() || consumer.version.isBlank() || dynamic(producer.version) || dynamic(consumer.version)
                ? VersionComparison.Result.UNRESOLVED : producer.version.equals(consumer.version) ? VersionComparison.Result.MATCH : VersionComparison.Result.MISMATCH;
        VersionComparison comparison = new VersionComparison(contract, producer.version, consumer.version, result,
                List.of(producer.evidence.evidenceId(), consumer.evidence.evidenceId()));
        String type = switch (result) { case MATCH -> "contract-version-exact-match"; case MISMATCH -> "contract-version-explicit-mismatch"; case UNRESOLVED -> "contract-version-compatibility-unresolved"; };
        String description = switch (result) {
            case MATCH -> "Producer and consumer reference " + contract + " version " + producer.version + ".";
            case MISMATCH -> "Producer references " + contract + " version " + producer.version + " while consumer references version " + consumer.version + ".";
            case UNRESOLVED -> "Producer/consumer version compatibility for " + contract + " is unresolved because version evidence is absent or dynamic.";
        };
        add(input, out, obs, type, contract + ":" + producer.evidence.identity() + ":" + consumer.evidence.identity(), description,
                result == VersionComparison.Result.UNRESOLVED ? DiscoveryConfidence.inferred(0.7, "At least one version is absent or dynamic.")
                        : DiscoveryConfidence.observedFact("Both compared versions are explicit literals."), result != VersionComparison.Result.UNRESOLVED,
                comparison.evidenceIds(), StructuralAnalysisSupport.details("contractId", contract, "producerVersion", comparison.producerVersion(),
                        "consumerVersion", comparison.consumerVersion(), "comparison", comparison.result().name()));
    }
    private static void add(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, String type, String identity,
                            String description, DiscoveryConfidence confidence, boolean observed, List<String> ids, Map<String, String> details) {
        DiscoveryEvidence e = StructuralAnalysisSupport.evidence(ID, input, type, identity, confidence, observed, ids, details);
        out.add(e); obs.add(StructuralAnalysisSupport.observation(ID, type, description, e));
    }
    private static boolean isProducer(DiscoveryEvidence e) { return e.evidenceType().contains("producer-reference") || e.evidenceType().equals("asyncapi-producer"); }
    private static boolean isConsumer(DiscoveryEvidence e) { return e.evidenceType().contains("consumer-reference") || e.evidenceType().equals("asyncapi-consumer"); }
    private static boolean isContract(DiscoveryEvidence e) { return e.evidenceType().contains("contract") && !e.evidenceType().contains("ownership"); }
    private static String contractId(DiscoveryEvidence e) { return first(e.attributes().get("contractId"), e.attributes().get("eventName"), e.attributes().get("commandName")); }
    private static String version(DiscoveryEvidence e) { return first(e.attributes().get("contractVersion"), e.attributes().get("version"), e.attributes().get("schemaVersion")); }
    private static boolean dynamic(String v) { return v.contains("${") || v.contains("#{"); }
    private static String first(String... values) { for (String v : values) if (v != null && !v.isBlank()) return v; return ""; }
    private static String capitalize(String value) { return Character.toUpperCase(value.charAt(0)) + value.substring(1); }
    private record VersionRef(DiscoveryEvidence evidence, String version) { }
    private static final class ContractState { final String id; final Set<String> versions = new LinkedHashSet<>(); final List<DiscoveryEvidence> definitions = new ArrayList<>(), deprecated = new ArrayList<>(), contractEvidence = new ArrayList<>(); final List<VersionRef> producers = new ArrayList<>(), consumers = new ArrayList<>();
        ContractState(String id) { this.id = id; } List<String> allIds() { List<String> ids = new ArrayList<>(); definitions.forEach(e -> ids.add(e.evidenceId())); contractEvidence.forEach(e -> ids.add(e.evidenceId())); return ids.stream().distinct().sorted().toList(); } }
}
