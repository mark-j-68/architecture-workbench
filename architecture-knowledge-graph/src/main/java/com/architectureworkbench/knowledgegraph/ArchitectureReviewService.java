package com.architectureworkbench.knowledgegraph;

import java.util.Map;

public class ArchitectureReviewService {
    private final ArchitectureElementService elementService;
    private final RelationshipService relationshipService;
    private final ImmutableKnowledgeGraphAuditLog auditLog;

    public ArchitectureReviewService(ArchitectureElementService elementService, RelationshipService relationshipService, ImmutableKnowledgeGraphAuditLog auditLog) {
        this.elementService = elementService;
        this.relationshipService = relationshipService;
        this.auditLog = auditLog;
    }

    public ArchitectureReview recordReviewFinding(ArchitectureKnowledgeGraph graph, RecordReviewFindingCommand command) {
        if (graph.element(command.reviewedElementId()).isEmpty()) {
            throw new IllegalArgumentException("Reviewed element does not exist: " + command.reviewedElementId().value());
        }
        ArchitectureReview review = (ArchitectureReview) elementService.createElement(graph, new CreateArchitectureElementCommand(
                ArchitectureElementType.ARCHITECTURE_REVIEW,
                "Review finding: " + command.reviewedElementId().value(),
                command.finding(),
                command.attributes(),
                command.actorRef()
        ));
        relationshipService.linkElements(graph, new LinkElementsCommand(
                command.reviewedElementId(),
                review.id(),
                RelationshipType.REVIEWED_BY,
                "reviewed by",
                Map.of("reviewer", command.reviewer()),
                command.actorRef()
        ));
        auditLog.append(graph.graphId(), command.actorRef(), "ARCHITECTURE_REVIEW_FINDING_RECORDED", review.id().value(),
                Map.of("reviewedElement", command.reviewedElementId().value(), "reviewer", command.reviewer()));
        return review;
    }
}
