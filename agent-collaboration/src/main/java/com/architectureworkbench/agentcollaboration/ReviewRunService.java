package com.architectureworkbench.agentcollaboration;

import com.architectureworkbench.audit.Actor;
import com.architectureworkbench.audit.ArchitectureEventEnvelope;
import com.architectureworkbench.audit.ArchitectureEventSource;
import com.architectureworkbench.audit.ArchitectureEventType;
import com.architectureworkbench.audit.AuditRelevance;
import com.architectureworkbench.audit.CausationId;
import com.architectureworkbench.audit.CorrelationId;
import com.architectureworkbench.audit.MutationTarget;
import com.architectureworkbench.audit.ReviewCompleted;
import com.architectureworkbench.audit.ReviewRequested;
import java.util.List;

public class ReviewRunService {
    private final List<ArchitectureReviewer> reviewers;
    private final ConsensusService consensusService;
    private final ImmutableAgentAuditLog auditLog;

    public ReviewRunService(List<ArchitectureReviewer> reviewers, ConsensusService consensusService, ImmutableAgentAuditLog auditLog) {
        if (reviewers == null || reviewers.isEmpty()) {
            throw new IllegalArgumentException("At least one reviewer is required.");
        }
        this.reviewers = List.copyOf(reviewers);
        this.consensusService = consensusService;
        this.auditLog = auditLog;
    }

    public ReviewRunResult runReview(ReviewRequest request) {
        CorrelationId correlationId = CorrelationId.newId("review");
        ReviewRequested requested = new ReviewRequested(request.reviewId(), reviewers.size(), false);
        auditLog.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.REVIEW_REQUESTED,
                request.workspaceId(),
                ArchitectureEventSource.AGENT_COLLABORATION,
                Actor.human(request.actorRef()),
                CausationId.newId("request-review"),
                correlationId,
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.NEITHER,
                requested.payload(),
                null
        ));
        List<ReviewResponse> responses = reviewers.stream()
                .map(reviewer -> reviewer.review(request))
                .toList();
        ReviewConsensus consensus = consensusService.generateConsensus(request, responses);
        ReviewCompleted completed = new ReviewCompleted(request.reviewId(), responses.size(), consensus.auditEventId(), false);
        auditLog.append(new ArchitectureEventEnvelope(
                null,
                ArchitectureEventType.REVIEW_COMPLETED,
                request.workspaceId(),
                ArchitectureEventSource.AGENT_COLLABORATION,
                Actor.human(request.actorRef()),
                CausationId.newId("complete-review"),
                correlationId,
                null,
                AuditRelevance.REQUIRED,
                MutationTarget.INTELLIGENCE_MODEL,
                completed.payload(),
                null
        ));
        return new ReviewRunResult(request.reviewId(), responses, consensus);
    }
}
