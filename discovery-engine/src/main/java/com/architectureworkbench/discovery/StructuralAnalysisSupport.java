package com.architectureworkbench.discovery;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StructuralAnalysisSupport {
    static final int MAX_NODES = 10_000;
    static final int MAX_EDGES = 50_000;
    static final int MAX_PATH_DEPTH = 32;
    static final int MAX_PATHS = 5_000;

    private StructuralAnalysisSupport() { }

    static List<DiscoveryEvidence> prior(DiscoveryInput input) {
        return input.priorOutputs().stream().flatMap(output -> output.evidence().stream())
                .sorted(Comparator.comparing(DiscoveryEvidence::evidenceId)).toList();
    }

    static DiscoveryEvidence evidence(DiscoveryPluginId pluginId, DiscoveryInput input, String type,
                                      String identity, DiscoveryConfidence confidence, boolean observed,
                                      Collection<String> sourceIds, Map<String, String> details) {
        Map<String, String> attributes = new LinkedHashMap<>(details);
        List<String> ids = sourceIds == null ? List.of() : sourceIds.stream().filter(id -> id != null && !id.isBlank()).distinct().sorted().toList();
        attributes.put("pluginId", pluginId.value());
        attributes.put("workspace", input.rootDirectory().toAbsolutePath().normalize().toString());
        attributes.put("repository", input.rootDirectory().getFileName() == null ? "." : input.rootDirectory().getFileName().toString());
        attributes.put("classification", observed ? "observed" : "inferred");
        attributes.put("supportingEvidenceIds", String.join(",", ids));
        attributes.putIfAbsent("derivation", confidence.rationale());
        attributes.putIfAbsent("explanation", attributes.get("derivation"));
        attributes.putIfAbsent("truncated", "false");
        return new DiscoveryEvidence(
                DiscoveryIdentity.stableId("structural-analysis-evidence", pluginId.value(), type, identity),
                type, pluginId.value(), "workspace:" + input.rootDirectory().toAbsolutePath().normalize(), identity,
                confidence, observed, Instant.EPOCH, ids, Map.copyOf(attributes));
    }

    static DiscoveryObservation observation(DiscoveryPluginId pluginId, String type, String description,
                                            DiscoveryEvidence analysisEvidence) {
        return new DiscoveryObservation(
                DiscoveryIdentity.stableId("structural-analysis-observation", pluginId.value(), type, analysisEvidence.identity()),
                type, description, analysisEvidence.confidence(),
                concat(analysisEvidence.evidenceId(), analysisEvidence.references()), Instant.EPOCH);
    }

    static DiscoveryPluginResult result(DiscoveryPluginId id, Instant started, List<DiscoveryEvidence> evidence,
                                        List<DiscoveryObservation> observations, List<String> diagnostics,
                                        boolean partial) {
        DiscoveryOutput output = new DiscoveryOutput(evidence, observations, diagnostics);
        Duration elapsed = Duration.between(started, Instant.now());
        return partial
                ? new DiscoveryPluginResult(id, DiscoveryPluginStatus.PARTIAL_SUCCESS, output, elapsed,
                    diagnostics.isEmpty() ? "Analysis completed partially." : String.join("; ", diagnostics))
                : DiscoveryPluginResult.succeeded(id, output, elapsed);
    }

    static Map<String, String> details(String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) result.put(values[i], values[i + 1] == null ? "" : values[i + 1]);
        return result;
    }

    static List<String> evidenceIds(Collection<DependencyEdge> edges) {
        return edges.stream().flatMap(edge -> edge.evidenceIds().stream()).distinct().sorted().toList();
    }

    static String edgeSequence(Collection<DependencyEdge> edges) {
        return edges.stream().map(DependencyEdge::sequence).sorted().reduce((a, b) -> a + " | " + b).orElse("");
    }

    static List<Set<String>> stronglyConnected(Collection<DependencyEdge> edges, List<String> diagnostics) {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        int count = 0;
        for (DependencyEdge edge : edges) {
            if (count++ >= MAX_EDGES) {
                diagnostics.add("Graph edge limit " + MAX_EDGES + " reached; cycle analysis was truncated.");
                break;
            }
            if (graph.size() >= MAX_NODES && !graph.containsKey(edge.from().id()) && !graph.containsKey(edge.to().id())) {
                diagnostics.add("Graph node limit " + MAX_NODES + " reached; cycle analysis was truncated.");
                break;
            }
            graph.computeIfAbsent(edge.from().id(), ignored -> new LinkedHashSet<>()).add(edge.to().id());
            graph.computeIfAbsent(edge.to().id(), ignored -> new LinkedHashSet<>());
        }
        List<Set<String>> components = iterativeComponents(graph);
        return components.stream().filter(component -> component.size() > 1 || selfLoop(component, graph))
                .sorted(Comparator.comparing(component -> component.stream().sorted().findFirst().orElse(""))).toList();
    }

    static List<TopologyPath> boundedPaths(List<TopologyEdge> edges, Set<String> starts, Set<String> ends,
                                           List<String> diagnostics) {
        Map<String, List<TopologyEdge>> outgoing = new LinkedHashMap<>();
        edges.stream().sorted(Comparator.comparing(edge -> edge.from().id() + "|" + edge.to().id()))
                .limit(MAX_EDGES).forEach(edge -> outgoing.computeIfAbsent(edge.from().id(), ignored -> new ArrayList<>()).add(edge));
        List<TopologyPath> result = new ArrayList<>();
        boolean[] truncated = {edges.size() > MAX_EDGES};
        for (String start : starts.stream().sorted().toList()) {
            walk(new TopologyNode(start, "start"), outgoing, ends, new ArrayList<>(), new ArrayList<>(), new HashSet<>(), result, truncated);
            if (result.size() >= MAX_PATHS) break;
        }
        if (truncated[0]) diagnostics.add("Topology traversal limit reached (depth " + MAX_PATH_DEPTH + ", paths " + MAX_PATHS + ").");
        return List.copyOf(result);
    }

    private static void walk(TopologyNode current, Map<String, List<TopologyEdge>> outgoing, Set<String> ends,
                             List<TopologyNode> nodes, List<TopologyEdge> pathEdges, Set<String> visited,
                             List<TopologyPath> result, boolean[] truncated) {
        if (result.size() >= MAX_PATHS) { truncated[0] = true; return; }
        nodes.add(current);
        if (ends.contains(current.id()) && nodes.size() > 1) result.add(new TopologyPath(nodes, pathEdges, false));
        if (pathEdges.size() >= MAX_PATH_DEPTH) { truncated[0] = true; return; }
        visited.add(current.id());
        for (TopologyEdge edge : outgoing.getOrDefault(current.id(), List.of())) {
            if (visited.contains(edge.to().id())) continue;
            List<TopologyNode> nextNodes = new ArrayList<>(nodes);
            List<TopologyEdge> nextEdges = new ArrayList<>(pathEdges); nextEdges.add(edge);
            walk(edge.to(), outgoing, ends, nextNodes, nextEdges, new HashSet<>(visited), result, truncated);
        }
    }

    private static boolean selfLoop(Set<String> component, Map<String, Set<String>> graph) {
        String node = component.iterator().next();
        return graph.getOrDefault(node, Set.of()).contains(node);
    }

    private static List<String> concat(String first, List<String> rest) {
        List<String> result = new ArrayList<>(); result.add(first); result.addAll(rest); return List.copyOf(result);
    }

    private static List<Set<String>> iterativeComponents(Map<String, Set<String>> graph) {
        List<String> finishOrder = new ArrayList<>(); Set<String> visited = new HashSet<>();
        for (String start : graph.keySet().stream().sorted().toList()) {
            if (!visited.add(start)) continue;
            Deque<String> nodes = new ArrayDeque<>(); Deque<Iterator<String>> iterators = new ArrayDeque<>();
            nodes.push(start); iterators.push(graph.getOrDefault(start, Set.of()).stream().sorted().iterator());
            while (!nodes.isEmpty()) {
                Iterator<String> iterator = iterators.peek();
                if (iterator.hasNext()) {
                    String target = iterator.next();
                    if (visited.add(target)) { nodes.push(target); iterators.push(graph.getOrDefault(target, Set.of()).stream().sorted().iterator()); }
                } else { finishOrder.add(nodes.pop()); iterators.pop(); }
            }
        }
        Map<String, Set<String>> reverse = new LinkedHashMap<>(); graph.keySet().forEach(node -> reverse.put(node, new LinkedHashSet<>()));
        graph.forEach((from, targets) -> targets.forEach(to -> reverse.computeIfAbsent(to, ignored -> new LinkedHashSet<>()).add(from)));
        List<Set<String>> components = new ArrayList<>(); visited.clear();
        for (int i = finishOrder.size() - 1; i >= 0; i--) {
            String start = finishOrder.get(i); if (!visited.add(start)) continue;
            Set<String> component = new LinkedHashSet<>(); Deque<String> stack = new ArrayDeque<>(); stack.push(start);
            while (!stack.isEmpty()) {
                String node = stack.pop(); component.add(node);
                reverse.getOrDefault(node, Set.of()).stream().sorted(Comparator.reverseOrder())
                        .filter(visited::add).forEach(stack::push);
            }
            components.add(Set.copyOf(component));
        }
        return components;
    }
}
