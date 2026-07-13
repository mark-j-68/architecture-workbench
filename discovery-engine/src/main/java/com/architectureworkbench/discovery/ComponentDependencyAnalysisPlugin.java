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

/** Aggregates directly observable component relationships and connectivity counts. */
public class ComponentDependencyAnalysisPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("analysis.component-dependency");
    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Component Dependency Analysis Plugin", "0.2.5", "Structural Analysis Plugin",
                List.of("java", "spring"), List.of(DiscoveryPluginCapability.ANALYZE_COMPONENT_DEPENDENCIES),
                List.of(new DiscoveryPluginDependency(JavaStructureDiscoveryPlugin.ID, true),
                        new DiscoveryPluginDependency(SpringComponentDiscoveryPlugin.ID, false)), true);
    }
    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now(); List<DiscoveryEvidence> out = new ArrayList<>(); List<DiscoveryObservation> obs = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>(); List<DiscoveryEvidence> prior = StructuralAnalysisSupport.prior(input);
        Map<String, DiscoveryEvidence> components = new LinkedHashMap<>();
        prior.stream().filter(e -> Set.of("spring-component", "spring-web-controller", "spring-data-repository", "spring-bean-method").contains(e.evidenceType()))
                .forEach(e -> components.putIfAbsent(e.attributes().getOrDefault("className", e.attributes().getOrDefault("symbol", "")), e));
        Map<String, DependencyEdge> edges = new LinkedHashMap<>();
        for (DiscoveryEvidence e : prior) {
            String from = "", to = "", kind = e.evidenceType();
            if (Set.of("spring-component-dependency", "spring-controller-service-dependency").contains(kind)) {
                from = e.attributes().getOrDefault("className", ""); to = e.attributes().getOrDefault("dependencyType", "");
            } else if (kind.equals("spring-component-interface")) {
                from = e.attributes().getOrDefault("className", ""); to = e.attributes().getOrDefault("interfaceType", "");
            } else if (kind.equals("java-import")) {
                from = e.attributes().getOrDefault("className", ""); String name = e.attributes().getOrDefault("importName", "");
                to = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name;
                if (!components.containsKey(from) || !components.containsKey(to)) continue;
            }
            if (from.isBlank() || to.isBlank()) continue;
            DependencyNode source = new DependencyNode(from, "component", e.attributes().getOrDefault("module", "."));
            DependencyNode target = new DependencyNode(to, "component", components.containsKey(to) ? components.get(to).attributes().getOrDefault("module", ".") : ".");
            edges.putIfAbsent(from + "->" + to, new DependencyEdge(source, target, kind, List.of(e.evidenceId())));
        }
        Set<String> all = new LinkedHashSet<>(components.keySet()); edges.values().forEach(e -> { all.add(e.from().id()); all.add(e.to().id()); });
        Map<String, Integer> fanIn = new HashMap<>(), fanOut = new HashMap<>(); edges.values().forEach(e -> { fanOut.merge(e.from().id(), 1, Integer::sum); fanIn.merge(e.to().id(), 1, Integer::sum); });
        for (String component : all.stream().filter(s -> !s.isBlank()).sorted().toList()) {
            List<DependencyEdge> related = edges.values().stream().filter(e -> e.from().id().equals(component) || e.to().id().equals(component)).toList();
            addMetric(input, out, obs, component, "component-fan-in", fanIn.getOrDefault(component, 0), related);
            addMetric(input, out, obs, component, "component-fan-out", fanOut.getOrDefault(component, 0), related);
            int connected = fanIn.getOrDefault(component, 0) + fanOut.getOrDefault(component, 0);
            if (connected >= 5) addMetric(input, out, obs, component, "highly-connected-component-count", connected, related);
        }
        for (DependencyEdge edge : edges.values().stream().sorted(Comparator.comparing(DependencyEdge::sequence)).toList()) {
            DiscoveryEvidence path = StructuralAnalysisSupport.evidence(ID, input, "component-dependency-path", edge.sequence(),
                    DiscoveryConfidence.high("Component edge follows from injection, implementation, bean, or direct import evidence."), false, edge.evidenceIds(),
                    StructuralAnalysisSupport.details("from", edge.from().id(), "to", edge.to().id(), "edgeSequence", edge.sequence(), "dependencyKind", edge.kind()));
            out.add(path); obs.add(StructuralAnalysisSupport.observation(ID, "component-dependency-path", edge.sequence() + ".", path));
            DependencyEdge reverse = edges.get(edge.to().id() + "->" + edge.from().id());
            if (reverse != null && edge.from().id().compareTo(edge.to().id()) < 0) {
                List<String> ids = new ArrayList<>(edge.evidenceIds()); ids.addAll(reverse.evidenceIds());
                DiscoveryEvidence both = StructuralAnalysisSupport.evidence(ID, input, "bidirectional-component-dependency", edge.from().id() + "<->" + edge.to().id(),
                        DiscoveryConfidence.high("Component dependencies are present in both directions."), false, ids,
                        StructuralAnalysisSupport.details("components", edge.from().id() + "," + edge.to().id(), "edgeSequence", edge.sequence() + " | " + reverse.sequence()));
                out.add(both); obs.add(StructuralAnalysisSupport.observation(ID, "bidirectional-component-dependency", edge.from().id() + " and " + edge.to().id() + " depend on each other.", both));
            }
        }
        return StructuralAnalysisSupport.result(ID, started, out, obs, diagnostics, false);
    }
    private static void addMetric(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, String component, String type, int value, List<DependencyEdge> edges) {
        DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, type, component, DiscoveryConfidence.observedFact("Counted distinct component dependency edges."), true,
                StructuralAnalysisSupport.evidenceIds(edges), StructuralAnalysisSupport.details("component", component, "value", Integer.toString(value), "unit", "count"));
        out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, type, component + " has " + type.replace("component-", "") + " " + value + ".", a));
    }
}
