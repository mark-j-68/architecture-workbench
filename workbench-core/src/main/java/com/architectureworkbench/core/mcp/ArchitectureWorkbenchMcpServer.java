package com.architectureworkbench.core.mcp;

import com.architectureworkbench.core.agent.ArchitectureContextBuilder;
import com.architectureworkbench.core.agent.ArchitectureReviewService;
import com.architectureworkbench.core.agent.ReviewKind;
import com.architectureworkbench.core.agent.ReviewRequest;
import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.validation.AggregateMustHaveRootEntityRule;
import com.architectureworkbench.core.validation.AiConsensusConfigurationRule;
import com.architectureworkbench.core.validation.ArchitectureValidator;
import com.architectureworkbench.core.validation.CommandMustTargetAggregateRule;
import com.architectureworkbench.core.validation.EventMustHavePublisherRule;
import com.architectureworkbench.core.validation.EventStormCanvasHasTypedStickiesRule;
import com.architectureworkbench.core.validation.RegulatoryAuditConfigurationRule;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Legacy M2 MCP facade over {@code ArchitectureModel}.
 *
 * <p>New MCP work must use the graph-centred {@code knowledge-graph-mcp}
 * module and {@code ArchitectureKnowledgeGraph} boundary.
 */
@Deprecated
public class ArchitectureWorkbenchMcpServer {
    private static final Set<String> FORBIDDEN_MUTATION_TOOLS = Set.of(
            "mutate_architecture_model",
            "update_architecture_model",
            "delete_architecture_model",
            "apply_model_patch"
    );

    private final ArchitectureReviewService reviewService;
    private final ArchitectureContextBuilder contextBuilder;
    private final ArchitectureValidator validator;

    public ArchitectureWorkbenchMcpServer(ArchitectureReviewService reviewService) {
        this.reviewService = reviewService;
        this.contextBuilder = new ArchitectureContextBuilder();
        this.validator = new ArchitectureValidator()
                .register(new AggregateMustHaveRootEntityRule())
                .register(new CommandMustTargetAggregateRule())
                .register(new EventMustHavePublisherRule())
                .register(new EventStormCanvasHasTypedStickiesRule())
                .register(new AiConsensusConfigurationRule())
                .register(new RegulatoryAuditConfigurationRule());
    }

    public List<McpToolDescriptor> listTools() {
        return List.of(
                tool("architecture_context", "Read summarized architecture model context.", false),
                tool("validate_architecture_model", "Run architecture and governance validation rules.", false),
                tool("run_architecture_review", "Run a Claude plus OpenAI/Codex architecture review.", false),
                tool("run_ddd_validation_review", "Run a Claude plus OpenAI/Codex DDD validation review.", false),
                tool("run_consensus_review", "Run a consensus review and append the outcome to the immutable audit log.", false),
                tool("review_history", "Retrieve immutable review history for a workspace.", false)
        );
    }

    public Object callTool(McpToolCall call) {
        if (FORBIDDEN_MUTATION_TOOLS.contains(call.name())) {
            throw new IllegalArgumentException("Direct model mutation is not exposed through MCP; use validated application services.");
        }
        return switch (call.name()) {
            case "architecture_context" -> contextBuilder.build(workspaceId(call), model(call));
            case "validate_architecture_model" -> validator.validate(model(call));
            case "run_architecture_review" -> reviewService.runArchitectureReview(request(call, ReviewKind.ARCHITECTURE));
            case "run_ddd_validation_review" -> reviewService.runDddValidationReview(request(call, ReviewKind.DDD_VALIDATION));
            case "run_consensus_review" -> reviewService.runConsensusReview(request(call, ReviewKind.CONSENSUS));
            case "review_history" -> reviewService.reviewHistory(workspaceId(call));
            default -> throw new IllegalArgumentException("Unknown MCP tool: " + call.name());
        };
    }

    private static McpToolDescriptor tool(String name, String description, boolean mutatesArchitectureModel) {
        return new McpToolDescriptor(
                name,
                description,
                Map.of(
                        "workspaceId", "string",
                        "actorRef", "string",
                        "model", "ArchitectureModel",
                        "question", "string"
                ),
                mutatesArchitectureModel
        );
    }

    private static ReviewRequest request(McpToolCall call, ReviewKind kind) {
        return new ReviewRequest(workspaceId(call), actorRef(call), kind, model(call), question(call));
    }

    private static String workspaceId(McpToolCall call) {
        return String.valueOf(call.arguments().getOrDefault("workspaceId", "default-workspace"));
    }

    private static String actorRef(McpToolCall call) {
        return String.valueOf(call.arguments().getOrDefault("actorRef", "mcp-agent"));
    }

    private static String question(McpToolCall call) {
        return String.valueOf(call.arguments().getOrDefault("question", ""));
    }

    private static ArchitectureModel model(McpToolCall call) {
        Object model = call.arguments().get("model");
        if (model instanceof ArchitectureModel architectureModel) {
            return architectureModel;
        }
        throw new IllegalArgumentException("MCP tool requires an ArchitectureModel argument named 'model'.");
    }
}
