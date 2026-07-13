package com.architectureworkbench.discovery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Derives cycle membership from explicit package dependency evidence without judging the cycle. */
public class PackageCycleAnalysisPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("analysis.package-cycle");

    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Package Cycle Analysis Plugin", "0.2.5", "Structural Analysis Plugin",
                List.of("java"), List.of(DiscoveryPluginCapability.ANALYZE_PACKAGE_CYCLES),
                List.of(new DiscoveryPluginDependency(PackageDependencyDiscoveryPlugin.ID, true)), true);
    }

    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now(); List<DiscoveryEvidence> output = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>(); List<String> diagnostics = new ArrayList<>();
        Map<String, DependencyNode> nodes = new LinkedHashMap<>(); List<DependencyEdge> edges = new ArrayList<>();
        for (DiscoveryEvidence item : StructuralAnalysisSupport.prior(input)) {
            if (!item.evidenceType().equals("package-dependency") || !item.attributes().getOrDefault("dependencyKind", "internal").equals("internal")) continue;
            String from = item.attributes().getOrDefault("sourcePackage", ""); String to = item.attributes().getOrDefault("targetPackage", "");
            if (from.isBlank() || to.isBlank()) continue;
            DependencyNode source = nodes.computeIfAbsent(from, ignored -> new DependencyNode(from, "package", item.attributes().getOrDefault("sourceModule", ".")));
            DependencyNode target = nodes.computeIfAbsent(to, ignored -> new DependencyNode(to, "package", item.attributes().getOrDefault("targetModule", ".")));
            edges.add(new DependencyEdge(source, target, "package-import", List.of(item.evidenceId())));
        }
        boolean truncated = edges.size() > StructuralAnalysisSupport.MAX_EDGES || nodes.size() > StructuralAnalysisSupport.MAX_NODES;
        for (Set<String> memberIds : StructuralAnalysisSupport.stronglyConnected(edges, diagnostics)) {
            List<DependencyNode> members = memberIds.stream().map(nodes::get).sorted(Comparator.comparing(DependencyNode::id)).toList();
            List<DependencyEdge> cycleEdges = edges.stream().filter(edge -> memberIds.contains(edge.from().id()) && memberIds.contains(edge.to().id()))
                    .sorted(Comparator.comparing(DependencyEdge::sequence)).toList();
            DependencyCycle cycle = new DependencyCycle(members, cycleEdges, truncated);
            Set<String> modules = new LinkedHashSet<>(); members.forEach(member -> modules.add(member.module()));
            String identity = String.join("<->", members.stream().map(DependencyNode::id).toList());
            DiscoveryEvidence analysis = StructuralAnalysisSupport.evidence(ID, input,
                    members.size() == 2 ? "direct-package-cycle" : "multi-package-cycle", identity,
                    DiscoveryConfidence.observedFact("Strongly connected packages follow from explicit package dependency edges."), true,
                    StructuralAnalysisSupport.evidenceIds(cycle.edges()), StructuralAnalysisSupport.details(
                            "cycleMembers", String.join(",", memberIds.stream().sorted().toList()),
                            "edgeSequence", StructuralAnalysisSupport.edgeSequence(cycle.edges()),
                            "moduleBoundaries", String.join(",", modules.stream().sorted().toList()),
                            "crossModule", Boolean.toString(modules.size() > 1), "truncated", Boolean.toString(cycle.truncated()),
                            "derivation", "Tarjan strongly connected component over internal package dependency edges."));
            output.add(analysis); observations.add(StructuralAnalysisSupport.observation(ID, "package-cycle-observed",
                    "Package cycle contains " + String.join(", ", memberIds.stream().sorted().toList()) + ". Edge sequence: "
                            + StructuralAnalysisSupport.edgeSequence(cycle.edges()) + ".", analysis));
        }
        return StructuralAnalysisSupport.result(ID, started, output, observations, diagnostics, truncated);
    }
}
