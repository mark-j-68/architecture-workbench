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

    public record DiscoveryRunSummary(
            String runId,
            String workspaceId,
            String sourceReference,
            String status,
            Instant startedAt,
            Instant completedAt,
            String correlationId,
            String causationId,
            int pluginExecutionCount,
            int evidenceCount,
            int observationCount,
            int metricCount,
            int warningCount,
            int failureCount,
            boolean partialSuccess
    ) {
    }

    public record DiscoveryRunDetails(
            DiscoveryRunSummary summary,
            List<DiscoveryPluginExecution> plugins,
            List<DiscoveryEvidenceView> evidence,
            List<DiscoveryObservationView> observations,
            List<DiscoveryMetricView> metrics,
            List<DiscoveryDiagnosticView> diagnostics,
            Map<String, Integer> confidenceDistribution
    ) {
    }

    public record DiscoveryPluginExecution(
            String pluginId,
            String name,
            String category,
            String status,
            Instant startedAt,
            Instant completedAt,
            int evidenceCount,
            int observationCount,
            int metricCount,
            List<String> warnings,
            List<String> errors,
            List<String> dependencies,
            boolean partialSuccess
    ) {
    }

    public record DiscoveryEvidenceView(
            String evidenceId,
            String type,
            String title,
            String summary,
            DiscoveryProvenanceView provenance,
            DiscoveryConfidenceView confidence,
            String classification,
            List<String> sourceEvidenceIds,
            String derivationSummary,
            String unresolvedValue,
            Map<String, String> attributes
    ) {
    }

    public record DiscoveryObservationView(
            String observationId,
            String observationType,
            String description,
            String pluginId,
            String module,
            String classification,
            DiscoveryConfidenceView confidence,
            String derivationSummary,
            List<String> supportingEvidenceIds
    ) {
    }

    public record DiscoveryMetricView(
            String metricId,
            String name,
            double value,
            String unit,
            String scope,
            String module,
            String pluginId,
            String derivationSummary,
            DiscoveryConfidenceView confidence,
            List<String> supportingEvidenceIds
    ) {
    }

    public record DiscoveryDiagnosticView(
            String diagnosticId,
            String pluginId,
            String severity,
            String message,
            Instant occurredAt
    ) {
    }

    public record DiscoveryProvenanceView(
            String repositoryRelativeFilePath,
            String module,
            String packageName,
            String symbol,
            Integer lineNumber,
            String source,
            String provenance
    ) {
    }

    public record DiscoveryConfidenceView(double value, int percentage, String band, String rationale) {
    }

    public record CreateProductRequest(String name, String description, String actorRef) {}
    public record UpdateProductMetadataRequest(String name, String description, String status, String actorRef) {}
    public record AddProductRepositoryRequest(String sourceIdentity, String sourceReference, String role, List<String> discoveryRunIds,
                                              Map<String,String> versionMetadata, Map<String,String> ownershipMetadata, String actorRef) {}
    public record AttachDiscoveryRunRequest(String discoveryRunId, String actorRef) {}
    public record CreateProductModuleRequest(String name, String description, String type, String actorRef) {}
    public record ProductView(String productId,String workspaceId,String name,String description,String status,long compositionVersion,
                              Instant createdAt,Instant updatedAt,List<ProductRepositoryView> repositories,List<ProductModuleView> modules) {}
    public record ProductRepositoryView(String repositoryId,String sourceIdentity,String sourceReference,String role,String moduleId,String status,
                                        List<String> discoveryRunIds,Map<String,String> versionMetadata,Map<String,String> ownershipMetadata,Instant addedAt) {}
    public record ProductModuleView(String moduleId,String name,String description,String type,Instant createdAt) {}
    public record RepositoryEvidenceContribution(String repositoryId,String sourceIdentity,String discoveryRunId,int evidenceCount,int observationCount,int metricCount) {}
    public record ProductEvidenceItem(String productEvidenceId,String repositoryId,String repositorySourceIdentity,String discoveryRunId,
                                      DiscoveryEvidenceView evidence) {}
    public record ProductObservationItem(String productObservationId,String repositoryId,String discoveryRunId,DiscoveryObservationView observation) {}
    public record ProductMetricItem(String productMetricId,String repositoryId,String discoveryRunId,DiscoveryMetricView metric) {}
    public record CrossRepositoryIdentity(String identityId,String identityType,String canonicalValue,String explicitVersion,String matchType,
                                          double confidence,List<String> repositoryIds,List<String> productEvidenceIds) {}
    public record IdentityConflict(String conflictId,String rule,String candidateValue,String explanation,double confidence,List<String> repositoryIds,List<String> productEvidenceIds) {}
    public record ProductRelationshipView(String relationshipId,String type,String description,String sourceRepositoryId,String targetRepositoryId,
                                          String technicalIdentity,double confidence,List<String> productEvidenceIds) {}
    public record ProductCompositionDiagnostic(String diagnosticId,String severity,String message,List<String> repositoryIds,List<String> evidenceIds) {}
    public record ProductCompositionMetrics(int repositoryCount,int productModuleCount,int discoveryRunCount,int evidenceCount,
                                            int crossRepositoryRelationshipCount,int sharedContractCount,int sharedChannelCount,int unresolvedIdentityCount,
                                            double ownershipMetadataCoverage,double explicitVersionMetadataCoverage,double repositoryModuleAssignmentCoverage) {}
    public record ProductCompositionView(String productId,String workspaceId,long compositionVersion,Instant generatedAt,
                                         List<RepositoryEvidenceContribution> contributions,List<ProductEvidenceItem> evidence,
                                         List<ProductObservationItem> observations,List<ProductMetricItem> repositoryMetrics,
                                         List<CrossRepositoryIdentity> identities,List<IdentityConflict> conflicts,
                                         List<ProductRelationshipView> relationships,ProductCompositionMetrics metrics,
                                         List<ProductCompositionDiagnostic> diagnostics) {}

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
