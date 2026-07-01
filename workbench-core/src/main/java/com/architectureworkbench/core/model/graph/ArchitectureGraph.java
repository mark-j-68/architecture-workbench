package com.architectureworkbench.core.model.graph;

import java.util.ArrayList;
import java.util.List;

public class ArchitectureGraph {
    private List<GraphNode> nodes = new ArrayList<>();
    private List<GraphEdge> edges = new ArrayList<>();
    public List<GraphNode> getNodes() { return nodes; }
    public List<GraphEdge> getEdges() { return edges; }
    public void addNode(GraphNode node) { nodes.add(node); }
    public void addEdge(GraphEdge edge) { edges.add(edge); }
}
