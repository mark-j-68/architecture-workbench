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

/** Calculates communication topology shape without interpreting centralization as a smell. */
public class MessagingTopologyAnalysisPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("analysis.messaging-topology");
    @Override public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Messaging Topology Analysis Plugin", "0.2.5", "Structural Analysis Plugin",
                List.of("eventbridge", "sqs", "sns", "kafka", "rabbitmq", "asyncapi"), List.of(DiscoveryPluginCapability.ANALYZE_MESSAGING_TOPOLOGY),
                List.of(new DiscoveryPluginDependency(MessagingTopologyDiscoveryPlugin.ID, true)), true);
    }
    @Override public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now(); List<DiscoveryEvidence> out = new ArrayList<>(); List<DiscoveryObservation> obs = new ArrayList<>(); List<String> diagnostics = new ArrayList<>();
        List<TopologyEdge> edges = new ArrayList<>(); Map<String, String> kinds = new HashMap<>(); Map<String, DiscoveryEvidence> sources = new LinkedHashMap<>();
        Set<String> producers = new LinkedHashSet<>(), consumers = new LinkedHashSet<>(), channels = new LinkedHashSet<>(), events = new LinkedHashSet<>(), commands = new LinkedHashSet<>();
        List<DiscoveryEvidence> infra = new ArrayList<>();
        for (DiscoveryEvidence e : StructuralAnalysisSupport.prior(input)) {
            if (e.evidenceType().equals("messaging-infrastructure-topology")) { infra.add(e); continue; }
            if (!e.evidenceType().startsWith("topology-")) continue; String from = e.attributes().getOrDefault("from", ""), to = e.attributes().getOrDefault("to", "");
            if (from.isBlank() || to.isBlank()) continue; String relation = e.attributes().getOrDefault("relationship", e.evidenceType());
            String fromKind = relation.equals("PRODUCES_TO") ? "producer" : relation.equals("HANDLED_BY") ? "event" : relation.equals("PUBLISHES_COMMAND") ? "handler" : "node";
            String toKind = relation.equals("CONSUMED_BY") ? "consumer" : relation.equals("PRODUCES_TO") ? "channel" : relation.equals("PUBLISHES_COMMAND") ? "command" : "handler";
            TopologyEdge edge = new TopologyEdge(new TopologyNode(from, fromKind), new TopologyNode(to, toKind), relation, List.of(e.evidenceId())); edges.add(edge); sources.put(e.evidenceId(), e);
            kinds.put(from, fromKind); kinds.put(to, toKind); if (fromKind.equals("producer")) producers.add(from); if (toKind.equals("consumer")) consumers.add(to);
            if (fromKind.equals("channel")) channels.add(from); if (toKind.equals("channel")) channels.add(to); if (fromKind.equals("event")) events.add(from); if (toKind.equals("command")) commands.add(to);
        }
        aggregate(input, out, obs, "topology-producer-count", producers.size(), producers, edges); aggregate(input, out, obs, "topology-consumer-count", consumers.size(), consumers, edges);
        aggregate(input, out, obs, "topology-channel-count", channels.size(), channels, edges);
        for (String channel : channels.stream().sorted().toList()) {
            long fanIn = edges.stream().filter(e -> e.to().id().equals(channel)).count(), fanOut = edges.stream().filter(e -> e.from().id().equals(channel)).count();
            nodeMetric(input, out, obs, channel, "channel-fan-in", fanIn, edges); nodeMetric(input, out, obs, channel, "channel-fan-out", fanOut, edges);
            if (fanOut == 0) structural(input, out, obs, "channel-without-detected-consumer", channel, "Channel " + channel + " has no detected consumer.", edgesFor(channel, edges));
        }
        for (String consumer : consumers) {
            Set<String> inboundChannels = edges.stream().filter(e -> e.to().id().equals(consumer)).map(e -> e.from().id()).collect(java.util.stream.Collectors.toSet());
            boolean supplied = inboundChannels.stream().anyMatch(channel -> edges.stream().anyMatch(e -> e.to().id().equals(channel)));
            if (!supplied) structural(input, out, obs, "consumer-without-detected-producer", consumer, "Consumer " + consumer + " has no detected producer.", edgesFor(consumer, edges));
        }
        Set<String> starts = events.isEmpty() ? producers : events; Set<String> ends = commands.isEmpty() ? consumers : commands;
        List<TopologyPath> paths = StructuralAnalysisSupport.boundedPaths(edges, starts, ends, diagnostics);
        Map<String, Integer> participation = new HashMap<>();
        for (TopologyPath path : paths) {
            String sequence = path.nodes().stream().map(TopologyNode::id).reduce((a, b) -> a + " -> " + b).orElse("");
            List<String> ids = path.edges().stream().flatMap(e -> e.evidenceIds().stream()).distinct().toList();
            DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, "event-to-command-path", sequence,
                    DiscoveryConfidence.high("Path is a bounded traversal of explicit topology edges."), false, ids,
                    StructuralAnalysisSupport.details("path", sequence, "edgeSequence", sequence, "hopCount", Integer.toString(path.edges().size()), "truncated", Boolean.toString(path.truncated())));
            out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, "event-to-command-path", sequence + " has " + path.edges().size() + " hops.", a));
            path.nodes().stream().skip(1).limit(Math.max(0, path.nodes().size() - 2L)).forEach(node -> participation.merge(node.id(), 1, Integer::sum));
        }
        participation.entrySet().stream().filter(e -> e.getValue() >= 2).sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            List<TopologyEdge> related = edgesFor(entry.getKey(), edges); DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, "central-routing-node", entry.getKey(),
                    DiscoveryConfidence.high("The node occurs in multiple bounded event-to-command paths."), false,
                    related.stream().flatMap(e -> e.evidenceIds().stream()).toList(), StructuralAnalysisSupport.details("node", entry.getKey(), "routeCount", entry.getValue().toString(),
                            "derivation", "Counted bounded topology paths containing this intermediate node."));
            out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, "central-routing-node", entry.getKey() + " participates in " + entry.getValue() + " event-to-command paths.", a));
        });
        Map<String, List<DiscoveryEvidence>> infraByKind = infra.stream().collect(java.util.stream.Collectors.groupingBy(e -> e.attributes().getOrDefault("topologyKind", "unknown")));
        infraByKind.forEach((kind, evidence) -> structuralEvidence(input, out, obs, "topology-" + kind + "-present", kind,
                kind + " configuration is present.", evidence.stream().map(DiscoveryEvidence::evidenceId).toList()));
        boolean partial = diagnostics.stream().anyMatch(d -> d.contains("limit"));
        return StructuralAnalysisSupport.result(ID, started, out, obs, diagnostics, partial);
    }
    private static void aggregate(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, String type, int value, Set<String> nodes, List<TopologyEdge> edges) {
        List<String> ids = edges.stream().flatMap(e -> e.evidenceIds().stream()).distinct().toList(); DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, type, type,
                DiscoveryConfidence.observedFact("Counted distinct topology nodes."), true, ids, StructuralAnalysisSupport.details("value", Integer.toString(value), "nodes", String.join(",", nodes.stream().sorted().toList()), "unit", "count"));
        out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, type, type.replace('-', ' ') + " is " + value + ".", a));
    }
    private static void nodeMetric(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, String node, String type, long value, List<TopologyEdge> edges) {
        List<TopologyEdge> related = edgesFor(node, edges); DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, type, node,
                DiscoveryConfidence.observedFact("Counted incident topology edges."), true, related.stream().flatMap(e -> e.evidenceIds().stream()).toList(),
                StructuralAnalysisSupport.details("node", node, "value", Long.toString(value), "unit", "count")); out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, type, node + " has " + type + " " + value + ".", a));
    }
    private static void structural(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, String type, String node, String text, List<TopologyEdge> edges) {
        structuralEvidence(input, out, obs, type, node, text, edges.stream().flatMap(e -> e.evidenceIds().stream()).toList());
    }
    private static void structuralEvidence(DiscoveryInput input, List<DiscoveryEvidence> out, List<DiscoveryObservation> obs, String type, String identity, String text, List<String> ids) {
        DiscoveryEvidence a = StructuralAnalysisSupport.evidence(ID, input, type, identity, DiscoveryConfidence.high("Derived from the supplied, potentially incomplete topology evidence."), false, ids, Map.of()); out.add(a); obs.add(StructuralAnalysisSupport.observation(ID, type, text, a));
    }
    private static List<TopologyEdge> edgesFor(String node, List<TopologyEdge> edges) { return edges.stream().filter(e -> e.from().id().equals(node) || e.to().id().equals(node)).toList(); }
}
