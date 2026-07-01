package com.architectureworkbench.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record CreateWorkspaceRequest(String name, String actorRef) {
    }

    public record WorkspaceResponse(String id, String name, String graphId) {
    }

    public record GraphResponse(String graphId, List<ElementResponse> elements, List<RelationshipResponse> relationships) {
    }

    public record ElementResponse(String id, String type, String name, String description, Map<String, String> attributes) {
    }

    public record RelationshipResponse(
            String id,
            String sourceId,
            String targetId,
            String type,
            String label,
            Map<String, String> attributes
    ) {
    }

    public record RunLocalDiscoveryRequest(String path, String actorRef) {
    }

    public record DiscoveryRunResponse(
            String runId,
            String workspaceId,
            String graphId,
            Instant startedAt,
            Instant completedAt,
            int artifactCount,
            int findingCount,
            int recommendationCount,
            int proposedChangeCount
    ) {
    }

    public record FindingResponse(
            String id,
            String severity,
            String category,
            String description,
            double confidence,
            List<String> evidenceIds
    ) {
    }

    public record RecommendationResponse(
            String id,
            String description,
            String rationale,
            String estimatedImpact,
            String estimatedEffort,
            double confidence,
            String lifecycleStatus,
            List<String> findingIds
    ) {
    }

    public record ProposedChangeResponse(
            String id,
            String type,
            String status,
            String workspaceId,
            String correlationId,
            String recommendationId,
            List<String> findingIds,
            List<String> evidenceIds,
            Map<String, String> mutation
    ) {
    }

    public record OpenReviewBoardSessionRequest(
            List<String> recommendationIds,
            List<String> proposedChangeIds,
            List<ReviewBoardParticipantRequest> participants,
            String actorRef
    ) {
    }

    public record ReviewBoardParticipantRequest(String participantId, String name, String participantType) {
    }

    public record ReviewBoardSessionResponse(
            String sessionId,
            String workspaceId,
            String correlationId,
            String status,
            List<String> recommendationIds,
            List<String> proposedChangeIds,
            List<ReviewBoardParticipantResponse> participants,
            List<ReviewBoardVoteResponse> votes,
            ReviewBoardDecisionResponse decision
    ) {
    }

    public record ReviewBoardParticipantResponse(String participantId, String name, String participantType) {
    }

    public record RecordReviewBoardVoteRequest(String participantId, String voteType, String rationale) {
    }

    public record ReviewBoardVoteResponse(String voteId, String participantId, String voteType, String rationale, Instant votedAt) {
    }

    public record CloseReviewBoardSessionRequest(String actorRef) {
    }

    public record ReviewBoardDecisionResponse(String decisionType, String rationale, List<String> conditions, Instant decidedAt) {
    }

    public record DecideProposedChangeRequest(String workspaceId, String actorRef, String rationale) {
    }

    public record GenerateProjectionRequest(String type, String actorRef) {
    }

    public record ProjectionResponse(
            String type,
            Instant generatedAt,
            List<String> sourceElementRefs,
            List<String> sourceRelationshipRefs,
            Object payload
    ) {
    }

    public record ErrorResponse(String message) {
    }
}
