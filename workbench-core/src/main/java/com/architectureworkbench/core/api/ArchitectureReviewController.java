package com.architectureworkbench.core.api;

import com.architectureworkbench.core.agent.ArchitectureReviewService;
import com.architectureworkbench.core.agent.ConsensusReviewResult;
import com.architectureworkbench.core.agent.ReviewKind;
import com.architectureworkbench.core.agent.ReviewRequest;
import com.architectureworkbench.core.model.ArchitectureModel;
import java.util.List;

/**
 * Framework-neutral backend endpoint facade. Web adapters should route:
 * POST /api/reviews/architecture, POST /api/reviews/ddd, POST /api/reviews/consensus,
 * GET /api/reviews/history/{workspaceId} to these methods.
 */
public class ArchitectureReviewController {
    private final ArchitectureReviewService reviewService;

    public ArchitectureReviewController(ArchitectureReviewService reviewService) {
        this.reviewService = reviewService;
    }

    public ConsensusReviewResult runArchitectureReview(RunReviewCommand command) {
        return reviewService.runArchitectureReview(toRequest(command, ReviewKind.ARCHITECTURE));
    }

    public ConsensusReviewResult runDddValidationReview(RunReviewCommand command) {
        return reviewService.runDddValidationReview(toRequest(command, ReviewKind.DDD_VALIDATION));
    }

    public ConsensusReviewResult runConsensusReview(RunReviewCommand command) {
        return reviewService.runConsensusReview(toRequest(command, ReviewKind.CONSENSUS));
    }

    public List<ConsensusReviewResult> reviewHistory(String workspaceId) {
        return reviewService.reviewHistory(workspaceId);
    }

    private static ReviewRequest toRequest(RunReviewCommand command, ReviewKind kind) {
        return new ReviewRequest(command.workspaceId(), command.actorRef(), kind, command.model(), command.question());
    }

    public record RunReviewCommand(String workspaceId, String actorRef, ArchitectureModel model, String question) {}
}
