package com.architectureworkbench.discovery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Produces candidate technical layers and dependency directions, not layer-violation findings. */
public class LayerStructureAnalysisPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("analysis.layer-structure");
    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Layer Structure Analysis Plugin", "0.2.5", "Structural Analysis Plugin",
                List.of("java", "spring"), List.of(DiscoveryPluginCapability.ANALYZE_LAYER_STRUCTURE),
                List.of(new DiscoveryPluginDependency(JavaStructureDiscoveryPlugin.ID, true),
                        new DiscoveryPluginDependency(SpringComponentDiscoveryPlugin.ID, false),
                        new DiscoveryPluginDependency(SpringWebDiscoveryPlugin.ID, false),
                        new DiscoveryPluginDependency(SpringDataDiscoveryPlugin.ID, false)), true);
    }
    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now(); List<DiscoveryEvidence> out = new ArrayList<>(); List<DiscoveryObservation> obs = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>(); List<DiscoveryEvidence> prior = StructuralAnalysisSupport.prior(input);
        Map<String, LayerCandidate> layers = new LinkedHashMap<>();
        for (DiscoveryEvidence e : prior) candidate(e).ifPresent(candidate -> layers.merge(candidate.symbol(), candidate,
                (left, right) -> left.confidence().value() >= right.confidence().value() ? left : right));
        for (LayerCandidate layer : layers.values().stream().sorted(Comparator.comparing(LayerCandidate::symbol)).toList()) {
            DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, "candidate-layer", layer.symbol() + ":" + layer.name(),
                    layer.confidence(), layer.confidence().value() == 1.0, layer.evidenceIds(), StructuralAnalysisSupport.details(
                            "symbol", layer.symbol(), "candidateLayer", layer.name(),
                            "derivation", "Candidate layer inferred from an explicit framework marker or package naming evidence."));
            out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, "candidate-layer-inferred",
                    layer.symbol() + " is a candidate " + layer.name() + " layer element.", a));
        }
        for (DiscoveryEvidence e : prior) {
            if (e.evidenceType().equals("spring-component-dependency") || e.evidenceType().equals("spring-controller-service-dependency"))
                relation(input, e, e.attributes().getOrDefault("className", e.attributes().getOrDefault("symbol", "")),
                        e.attributes().getOrDefault("dependencyType", ""), layers, out, obs);
            if (e.evidenceType().equals("spring-data-repository-entity-association"))
                relation(input, e, e.attributes().getOrDefault("repositoryType", ""), e.attributes().getOrDefault("entityType", ""), layers, out, obs);
            if (e.evidenceType().equals("spring-transaction-boundary") && e.attributes().getOrDefault("componentKind", "").equals("controller")) {
                DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, "controller-transaction-boundary", e.identity(),
                        DiscoveryConfidence.observedFact("A controller transaction boundary is explicitly annotated."), true, List.of(e.evidenceId()),
                        StructuralAnalysisSupport.details("symbol", e.attributes().getOrDefault("symbol", e.identity()), "fromLayer", "controller",
                                "derivation", "Copied from explicit @Transactional evidence on a controller."));
                out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, "controller-transaction-boundary-observed",
                        "Transaction boundary found in controller " + e.attributes().getOrDefault("symbol", e.identity()) + ".", a));
            }
        }
        return StructuralAnalysisSupport.result(ID, started, out, obs, diagnostics, false);
    }
    private static java.util.Optional<LayerCandidate> candidate(DiscoveryEvidence e) {
        String symbol = e.attributes().getOrDefault("className", e.attributes().getOrDefault("packageName", "")); String layer = "";
        boolean explicit = true;
        switch (e.evidenceType()) {
            case "spring-web-controller" -> layer = "controller";
            case "spring-data-repository" -> layer = "repository";
            case "spring-data-entity" -> layer = "domain/entity";
            case "spring-component" -> layer = switch (e.attributes().getOrDefault("componentKind", "")) {
                case "service" -> "service"; case "repository" -> "repository"; case "configuration" -> "configuration"; default -> "component"; };
            case "java-package" -> { symbol = e.attributes().getOrDefault("packageName", e.identity()); layer = namedLayer(symbol); explicit = false; }
            default -> { return java.util.Optional.empty(); }
        }
        if (layer.isBlank() || symbol.isBlank()) return java.util.Optional.empty();
        DiscoveryConfidence confidence = explicit ? DiscoveryConfidence.observedFact("Explicit framework evidence identifies the candidate technical role.")
                : DiscoveryConfidence.inferred(0.7, "Package segment matches a conventional technical layer name.");
        return java.util.Optional.of(new LayerCandidate(layer, symbol, confidence, List.of(e.evidenceId())));
    }
    private static String namedLayer(String pkg) {
        String p = "." + pkg.toLowerCase(Locale.ROOT) + ".";
        for (String layer : List.of("controller", "service", "repository", "configuration", "adapter", "port", "domain", "entity"))
            if (p.contains("." + layer + ".") || p.endsWith("." + layer + ".")) return layer.equals("entity") ? "domain/entity" : layer;
        return "";
    }
    private static void relation(DiscoveryInput input, DiscoveryEvidence source, String from, String to, Map<String, LayerCandidate> layers,
                                 List<DiscoveryEvidence> out, List<DiscoveryObservation> obs) {
        if (from.isBlank() || to.isBlank()) return; LayerCandidate fromLayer = layers.get(from), toLayer = layers.get(to);
        String sourceLayer = fromLayer == null ? "unclassified" : fromLayer.name(); String targetLayer = toLayer == null ? inferredByName(to) : toLayer.name();
        List<String> ids = new ArrayList<>(List.of(source.evidenceId())); if (fromLayer != null) ids.addAll(fromLayer.evidenceIds()); if (toLayer != null) ids.addAll(toLayer.evidenceIds());
        DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, "layer-dependency-direction", from + "->" + to,
                DiscoveryConfidence.high("Direction follows from an explicit dependency; layer roles may be candidate classifications."), false, ids,
                StructuralAnalysisSupport.details("from", from, "to", to, "fromLayer", sourceLayer, "toLayer", targetLayer,
                        "edgeSequence", from + " -> " + to, "derivation", "Joined dependency evidence to explicit or candidate technical layer roles."));
        out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, directionType(sourceLayer, targetLayer),
                sourceLayer + " " + from + " depends on " + targetLayer + " " + to + ".", a));
    }
    private static String inferredByName(String value) { String lower = value.toLowerCase(Locale.ROOT); for (String x : List.of("controller", "service", "repository", "adapter", "port")) if (lower.endsWith(x)) return x; return "unclassified"; }
    private static String directionType(String from, String to) {
        if (from.equals("controller") && to.equals("repository")) return "controller-directly-depends-on-repository";
        return from + "-depends-on-" + to.replace('/', '-');
    }
}
