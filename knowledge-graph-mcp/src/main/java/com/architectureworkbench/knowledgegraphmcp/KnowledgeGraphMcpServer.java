package com.architectureworkbench.knowledgegraphmcp;

import com.architectureworkbench.knowledgegraph.ArchitectureElement;
import com.architectureworkbench.knowledgegraph.ArchitectureElementService;
import com.architectureworkbench.knowledgegraph.ArchitectureElementType;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.ArchitectureReview;
import com.architectureworkbench.knowledgegraph.CreateArchitectureElementCommand;
import com.architectureworkbench.knowledgegraph.DddConsistencyValidationService;
import com.architectureworkbench.knowledgegraph.ElementId;
import com.architectureworkbench.knowledgegraph.ProjectionService;
import com.architectureworkbench.knowledgegraph.ProjectionType;
import com.architectureworkbench.knowledgegraph.Relationship;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KnowledgeGraphMcpServer {
    private static final Set<String> FORBIDDEN_TOOLS = Set.of(
            "mutate_graph",
            "apply_graph_patch",
            "delete_graph_element",
            "update_graph_element",
            "raw_graph_write"
    );

    private final ArchitectureElementService elementService;
    private final ProjectionService projectionService;
    private final DddConsistencyValidationService validationService;

    public KnowledgeGraphMcpServer(
            ArchitectureElementService elementService,
            ProjectionService projectionService,
            DddConsistencyValidationService validationService
    ) {
        this.elementService = elementService;
        this.projectionService = projectionService;
        this.validationService = validationService;
    }

    public List<KnowledgeGraphMcpToolDescriptor> listTools() {
        return List.of(
                tool("read_graph", "Read a graph snapshot.", false, Map.of("graph", "ArchitectureKnowledgeGraph")),
                tool("find_element", "Find a graph element by id.", false, Map.of("graph", "ArchitectureKnowledgeGraph", "elementId", "string")),
                tool("list_relationships", "List graph relationships, optionally by element id.", false, Map.of("graph", "ArchitectureKnowledgeGraph", "elementId", "string?")),
                tool("validate_graph", "Run graph DDD consistency validation.", false, Map.of("graph", "ArchitectureKnowledgeGraph")),
                tool("generate_projection", "Generate a typed graph projection.", false, Map.of("graph", "ArchitectureKnowledgeGraph", "projectionType", "ProjectionType", "actorRef", "string")),
                tool("create_review_proposal", "Create a governed ArchitectureReview proposal through application services.", true, Map.of("graph", "ArchitectureKnowledgeGraph", "title", "string", "finding", "string", "recommendation", "string", "actorRef", "string"))
        );
    }

    public Object callTool(KnowledgeGraphMcpToolCall call) {
        if (FORBIDDEN_TOOLS.contains(call.name())) {
            throw new IllegalArgumentException("Uncontrolled graph mutation is not exposed through MCP; use validated application services.");
        }
        return switch (call.name()) {
            case "read_graph" -> snapshot(graph(call));
            case "find_element" -> graph(call).element(ElementId.of(requiredString(call, "elementId"))).orElse(null);
            case "list_relationships" -> listRelationships(call);
            case "validate_graph" -> validationService.validate(graph(call));
            case "generate_projection" -> projectionService.generateProjection(graph(call), ProjectionType.valueOf(requiredString(call, "projectionType")), stringArg(call, "actorRef", "mcp-agent"));
            case "create_review_proposal" -> createReviewProposal(call);
            default -> throw new IllegalArgumentException("Unknown knowledge graph MCP tool: " + call.name());
        };
    }

    private ArchitectureReview createReviewProposal(KnowledgeGraphMcpToolCall call) {
        ArchitectureElement review = elementService.createElement(graph(call), new CreateArchitectureElementCommand(
                ArchitectureElementType.ARCHITECTURE_REVIEW,
                requiredString(call, "title"),
                requiredString(call, "finding"),
                Map.of(
                        "recommendation", stringArg(call, "recommendation", ""),
                        "proposalSource", "knowledge-graph-mcp",
                        "status", "PROPOSED"
                ),
                stringArg(call, "actorRef", "mcp-agent")
        ));
        return (ArchitectureReview) review;
    }

    private List<Relationship> listRelationships(KnowledgeGraphMcpToolCall call) {
        ArchitectureKnowledgeGraph graph = graph(call);
        String elementId = stringArg(call, "elementId", "");
        if (elementId.isBlank()) {
            return List.copyOf(graph.relationships());
        }
        ElementId id = ElementId.of(elementId);
        return graph.relationships().stream()
                .filter(relationship -> relationship.sourceId().equals(id) || relationship.targetId().equals(id))
                .toList();
    }

    private static GraphSnapshot snapshot(ArchitectureKnowledgeGraph graph) {
        return new GraphSnapshot(
                graph.graphId(),
                graph.elements().stream()
                        .map(element -> new GraphSnapshot.ElementView(element.id().value(), element.type().name(), element.name(), element.description()))
                        .toList(),
                graph.relationships().stream()
                        .map(relationship -> new GraphSnapshot.RelationshipView(relationship.id(), relationship.sourceId().value(), relationship.targetId().value(), relationship.type().name(), relationship.label()))
                        .toList()
        );
    }

    private static KnowledgeGraphMcpToolDescriptor tool(String name, String description, boolean mutatesGraph, Map<String, String> inputSchema) {
        return new KnowledgeGraphMcpToolDescriptor(name, description, inputSchema, mutatesGraph);
    }

    private static ArchitectureKnowledgeGraph graph(KnowledgeGraphMcpToolCall call) {
        Object graph = call.arguments().get("graph");
        if (graph instanceof ArchitectureKnowledgeGraph architectureKnowledgeGraph) {
            return architectureKnowledgeGraph;
        }
        throw new IllegalArgumentException("MCP tool requires an ArchitectureKnowledgeGraph argument named 'graph'.");
    }

    private static String requiredString(KnowledgeGraphMcpToolCall call, String key) {
        String value = stringArg(call, key, "");
        if (value.isBlank()) {
            throw new IllegalArgumentException("MCP tool requires argument '%s'.".formatted(key));
        }
        return value;
    }

    private static String stringArg(KnowledgeGraphMcpToolCall call, String key, String defaultValue) {
        return String.valueOf(call.arguments().getOrDefault(key, defaultValue));
    }
}
