package com.architectureworkbench.discovery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compares declared Maven dependencies with observed cross-module source references. */
public class ModuleDependencyAnalysisPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("analysis.module-dependency");
    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Module Dependency Analysis Plugin", "0.2.5", "Structural Analysis Plugin",
                List.of("maven", "java"), List.of(DiscoveryPluginCapability.ANALYZE_MODULE_DEPENDENCIES),
                List.of(new DiscoveryPluginDependency(MavenDiscoveryPlugin.ID, true),
                        new DiscoveryPluginDependency(PackageDependencyDiscoveryPlugin.ID, false)), true);
    }
    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now(); List<DiscoveryEvidence> out = new ArrayList<>(); List<DiscoveryObservation> obs = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>(); List<DiscoveryEvidence> prior = StructuralAnalysisSupport.prior(input);
        Map<String, String> artifactToModule = new HashMap<>();
        prior.stream().filter(e -> e.evidenceType().equals("build-module")).forEach(e ->
                artifactToModule.put(e.attributes().getOrDefault("artifactId", ""), moduleFromPath(firstRef(e))));
        Map<String, EdgeSources> pairs = new LinkedHashMap<>();
        for (DiscoveryEvidence e : prior) {
            if (e.evidenceType().equals("module-package-reference")) add(pairs, e.attributes().get("sourceModule"), e.attributes().get("targetModule"), e, false);
            if (e.evidenceType().equals("dependency-declaration")) {
                String target = artifactToModule.getOrDefault(e.attributes().getOrDefault("artifactId", ""), "");
                if (!target.isBlank()) add(pairs, moduleFromPath(firstRef(e)), target, e, true);
            }
        }
        Set<String> modules = new LinkedHashSet<>(); pairs.values().forEach(pair -> { modules.add(pair.from); modules.add(pair.to); });
        for (EdgeSources pair : pairs.values().stream().sorted(Comparator.comparing(p -> p.from + "->" + p.to)).toList()) {
            String kind = pair.declared.isEmpty() ? "observed-use-without-declaration" : pair.used.isEmpty() ? "declared-without-observed-use" : "declared-and-observed-module-dependency";
            List<String> ids = pair.ids(); DiscoveryConfidence confidence = pair.declared.isEmpty() || pair.used.isEmpty()
                    ? DiscoveryConfidence.high("Comparison is deterministic, but incomplete source evidence may explain the absence.")
                    : DiscoveryConfidence.observedFact("The module dependency is both declared and observed in source evidence.");
            DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, kind, pair.from + "->" + pair.to, confidence,
                    !pair.declared.isEmpty() && !pair.used.isEmpty(), ids, StructuralAnalysisSupport.details("sourceModule", pair.from, "targetModule", pair.to,
                            "declared", Boolean.toString(!pair.declared.isEmpty()), "observedSourceUse", Boolean.toString(!pair.used.isEmpty()),
                            "derivation", "Compared internal Maven artifact declarations with cross-module Java package references."));
            out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, kind,
                    "Module " + pair.from + " references module " + pair.to + "; declared=" + !pair.declared.isEmpty()
                            + ", observed source use=" + !pair.used.isEmpty() + ".", a));
        }
        for (String module : modules.stream().sorted().toList()) {
            Set<String> outgoing = new LinkedHashSet<>(), incoming = new LinkedHashSet<>(); List<String> ids = new ArrayList<>();
            pairs.values().forEach(p -> { if (p.from.equals(module)) { outgoing.add(p.to); ids.addAll(p.ids()); } if (p.to.equals(module)) { incoming.add(p.from); ids.addAll(p.ids()); } });
            metric(input, out, obs, module, "module-fan-out", outgoing.size(), ids); metric(input, out, obs, module, "module-fan-in", incoming.size(), ids);
            for (String target : outgoing) if (pairs.containsKey(target + "->" + module) && module.compareTo(target) < 0) {
                List<String> both = new ArrayList<>(pairs.get(module + "->" + target).ids()); both.addAll(pairs.get(target + "->" + module).ids());
                DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, "bidirectional-module-reference", module + "<->" + target,
                        DiscoveryConfidence.observedFact("Evidence contains module references in both directions."), true, both,
                        StructuralAnalysisSupport.details("modules", module + "," + target, "edgeSequence", module + " -> " + target + " | " + target + " -> " + module));
                out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, "bidirectional-module-reference", "Modules " + module + " and " + target + " reference each other.", a));
            }
        }
        return StructuralAnalysisSupport.result(ID, started, out, obs, diagnostics, false);
    }
    private static void metric(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, String module, String type, int value, List<String> ids) {
        DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, type, module, DiscoveryConfidence.observedFact("Counted distinct module dependency edges."), true, ids,
                StructuralAnalysisSupport.details("module", module, "value", Integer.toString(value), "unit", "count"));
        out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, type, module + " has " + type.replace("module-", "") + " " + value + ".", a));
    }
    private static void add(Map<String, EdgeSources> pairs, String from, String to, DiscoveryEvidence e, boolean declared) {
        if (from == null || to == null || from.isBlank() || to.isBlank() || from.equals(to)) return;
        EdgeSources pair = pairs.computeIfAbsent(from + "->" + to, ignored -> new EdgeSources(from, to));
        (declared ? pair.declared : pair.used).add(e.evidenceId());
    }
    private static String firstRef(DiscoveryEvidence e) { return e.references().isEmpty() ? "." : e.references().get(0); }
    private static String moduleFromPath(String path) { int slash = path.indexOf('/'); return slash < 0 || path.equals("pom.xml") ? "." : path.substring(0, slash); }
    private static final class EdgeSources { final String from, to; final List<String> declared = new ArrayList<>(), used = new ArrayList<>();
        EdgeSources(String from, String to) { this.from = from; this.to = to; }
        List<String> ids() { List<String> r = new ArrayList<>(declared); r.addAll(used); return r.stream().distinct().sorted().toList(); } }
}
