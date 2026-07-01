package com.architectureworkbench.core;

import com.architectureworkbench.core.agent.ArchitectureReviewService;
import com.architectureworkbench.core.agent.ReviewKind;
import com.architectureworkbench.core.agent.ReviewRequest;
import com.architectureworkbench.core.api.ArchitectureReviewController;
import com.architectureworkbench.core.audit.ImmutableActivityLog;
import com.architectureworkbench.core.audit.InMemoryEncryptedPayloadStore;
import com.architectureworkbench.core.mcp.ArchitectureWorkbenchMcpServer;
import com.architectureworkbench.core.mcp.McpToolCall;
import com.architectureworkbench.core.model.ArchitectureModel;
import com.architectureworkbench.core.model.consensus.ConsensusOutcome;
import com.architectureworkbench.core.model.domain.Aggregate;
import com.architectureworkbench.core.model.domain.BoundedContext;
import com.architectureworkbench.core.model.domain.Command;
import com.architectureworkbench.core.model.governance.AiJudge;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureReviewServiceTest {
    @Test
    void consensusReviewStoresTraceInImmutableAuditLog() {
        InMemoryEncryptedPayloadStore payloadStore = new InMemoryEncryptedPayloadStore();
        ImmutableActivityLog auditLog = new ImmutableActivityLog(payloadStore);
        ArchitectureReviewService service = new ArchitectureReviewService(auditLog);

        var result = service.runConsensusReview(new ReviewRequest(
                "workspace-1",
                "architect-1",
                ReviewKind.CONSENSUS,
                validModel(),
                "Can this architecture proceed?"
        ));

        assertEquals(ConsensusOutcome.NEEDS_REVISION, result.decision().getOutcome());
        assertFalse(result.disagreements().isEmpty());
        assertEquals(1, result.auditEnvelope().getProtectedPayloads().size());
        assertEquals(result.auditEnvelope().getEnvelopeHash(), auditLog.entriesForWorkspace("workspace-1").get(0).getEnvelopeHash());

        String protectedTrace = payloadStore.retrieve(result.auditEnvelope().getProtectedPayloads().get(0).getPayloadId());
        assertTrue(protectedTrace.contains("claudeAssessment"));
        assertTrue(protectedTrace.contains("openAiCodexAssessment"));
        assertTrue(protectedTrace.contains("toolsUsed"));
    }

    @Test
    void controllerExposesReviewHistoryEndpointFacade() {
        ArchitectureReviewService service = new ArchitectureReviewService(new ImmutableActivityLog(new InMemoryEncryptedPayloadStore()));
        ArchitectureReviewController controller = new ArchitectureReviewController(service);

        controller.runArchitectureReview(new ArchitectureReviewController.RunReviewCommand("workspace-2", "architect-1", validModel(), ""));

        assertEquals(1, controller.reviewHistory("workspace-2").size());
    }

    @Test
    void mcpServerRejectsDirectModelMutationTools() {
        ArchitectureReviewService service = new ArchitectureReviewService(new ImmutableActivityLog(new InMemoryEncryptedPayloadStore()));
        ArchitectureWorkbenchMcpServer mcpServer = new ArchitectureWorkbenchMcpServer(service);

        assertTrue(mcpServer.listTools().stream().noneMatch(McpToolDescriptor -> McpToolDescriptor.mutatesArchitectureModel()));
        assertThrows(IllegalArgumentException.class, () -> mcpServer.callTool(new McpToolCall("apply_model_patch", Map.of())));
    }

    private static ArchitectureModel validModel() {
        ArchitectureModel model = new ArchitectureModel();
        BoundedContext context = new BoundedContext();
        context.setName("Mortgage Origination");
        Aggregate aggregate = new Aggregate();
        aggregate.setName("MortgageApplication");
        aggregate.setRootEntity("MortgageApplication");
        Command command = new Command();
        command.setName("SubmitMortgageApplication");
        command.setHandledByAggregate("MortgageApplication");
        context.getAggregates().add(aggregate);
        context.getCommands().add(command);
        model.getDomain().getBoundedContexts().add(context);

        AiJudge claude = new AiJudge();
        claude.setId("claude");
        claude.setProvider("CLAUDE");
        claude.setModel("claude-reviewer-adapter");
        claude.setEnabled(true);
        AiJudge codex = new AiJudge();
        codex.setId("codex");
        codex.setProvider("OPENAI_CODEX");
        codex.setModel("codex-reviewer-adapter");
        codex.setEnabled(true);
        model.getGovernance().getAi().getJudges().add(claude);
        model.getGovernance().getAi().getJudges().add(codex);
        return model;
    }
}
