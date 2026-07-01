package com.architectureworkbench.knowledgegraph;

import com.architectureworkbench.audit.CorrelationId;
import java.util.List;

public class ProposedChangeService {
    private final ArchitectureElementService elementService;
    private final RelationshipService relationshipService;

    public ProposedChangeService(ArchitectureElementService elementService, RelationshipService relationshipService) {
        this.elementService = elementService;
        this.relationshipService = relationshipService;
    }

    public ProposedArchitectureChange proposeElementAddition(
            String workspaceId,
            CorrelationId correlationId,
            ProposedElementAddition mutation,
            String recommendationId,
            List<String> findingIds,
            List<String> evidenceIds
    ) {
        return new ProposedArchitectureChange(
                null,
                ProposedChangeType.ELEMENT_ADDITION,
                ProposedChangeStatus.PROPOSED,
                mutation,
                workspaceId,
                correlationId,
                recommendationId,
                findingIds,
                evidenceIds,
                null,
                null,
                ""
        );
    }

    public ProposedArchitectureChange proposeRelationshipAddition(
            String workspaceId,
            CorrelationId correlationId,
            ProposedRelationshipAddition mutation,
            String recommendationId,
            List<String> findingIds,
            List<String> evidenceIds
    ) {
        return new ProposedArchitectureChange(
                null,
                ProposedChangeType.RELATIONSHIP_ADDITION,
                ProposedChangeStatus.PROPOSED,
                mutation,
                workspaceId,
                correlationId,
                recommendationId,
                findingIds,
                evidenceIds,
                null,
                null,
                ""
        );
    }

    public ProposedArchitectureChange acceptProposedChange(
            ArchitectureKnowledgeGraph graph,
            ProposedArchitectureChange change,
            String actorRef,
            String rationale
    ) {
        if (change.status() == ProposedChangeStatus.REJECTED) {
            throw new IllegalStateException("Rejected proposed changes cannot be accepted.");
        }
        if (change.status() == ProposedChangeStatus.ACCEPTED) {
            throw new IllegalStateException("Proposed change is already accepted.");
        }
        switch (change.mutation()) {
            case ProposedElementAddition elementAddition -> elementService.createElement(graph, new CreateArchitectureElementCommand(
                    elementAddition.elementType(),
                    elementAddition.name(),
                    elementAddition.description(),
                    elementAddition.attributes(),
                    actorRef
            ));
            case ProposedRelationshipAddition relationshipAddition -> relationshipService.linkElements(graph, new LinkElementsCommand(
                    relationshipAddition.sourceId(),
                    relationshipAddition.targetId(),
                    relationshipAddition.relationshipType(),
                    relationshipAddition.label(),
                    relationshipAddition.attributes(),
                    actorRef
            ));
        }
        return change.withStatus(ProposedChangeStatus.ACCEPTED, rationale);
    }

    public ProposedArchitectureChange rejectProposedChange(ProposedArchitectureChange change, String rationale) {
        if (change.status() == ProposedChangeStatus.ACCEPTED) {
            throw new IllegalStateException("Accepted proposed changes cannot be rejected.");
        }
        return change.withStatus(ProposedChangeStatus.REJECTED, rationale);
    }

    public ProposedArchitectureChange deferProposedChange(ProposedArchitectureChange change, String rationale) {
        if (change.status() == ProposedChangeStatus.ACCEPTED) {
            throw new IllegalStateException("Accepted proposed changes cannot be deferred.");
        }
        return change.withStatus(ProposedChangeStatus.DEFERRED, rationale);
    }
}
