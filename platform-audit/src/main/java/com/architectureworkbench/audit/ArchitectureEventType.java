package com.architectureworkbench.audit;

public enum ArchitectureEventType {
    WORKSPACE_CREATED("WorkspaceCreated"),
    GRAPH_IMPORTED("GraphImported"),
    ELEMENT_ADDED("ElementAdded"),
    RELATIONSHIP_ADDED("RelationshipAdded"),
    PROJECTION_GENERATED("ProjectionGenerated"),
    DISCOVERY_STARTED("DiscoveryStarted"),
    DISCOVERY_COMPLETED("DiscoveryCompleted"),
    REVIEW_REQUESTED("ReviewRequested"),
    REVIEW_COMPLETED("ReviewCompleted"),
    PROVIDER_INVOKED("ProviderInvoked"),
    MCP_TOOL_INVOKED("McpToolInvoked"),
    HYPOTHESIS_CREATED("HypothesisCreated"),
    RECOMMENDATION_PROPOSED("RecommendationProposed"),
    DECISION_RECORDED("DecisionRecorded"),
    EXPERIMENT_STARTED("ExperimentStarted"),
    OUTCOME_RECORDED("OutcomeRecorded"),
    LEARNING_DERIVED("LearningDerived"),
    PATTERN_PUBLISHED("PatternPublished"),
    PRODUCT_CREATED("ProductCreated"),
    PRODUCT_REPOSITORY_ADDED("ProductRepositoryAdded"),
    PRODUCT_REPOSITORY_REMOVED("ProductRepositoryRemoved"),
    PRODUCT_MODULE_CREATED("ProductModuleCreated"),
    PRODUCT_REPOSITORY_ASSIGNED_TO_MODULE("ProductRepositoryAssignedToModule"),
    PRODUCT_COMPOSITION_GENERATED("ProductCompositionGenerated"),
    PRODUCT_DEPENDENCY_COMPOSITION_STARTED("ProductDependencyCompositionStarted"),
    PRODUCT_ARCHITECTURE_ANALYSIS_STARTED("ProductArchitectureAnalysisStarted"),
    PRODUCT_ARCHITECTURE_ANALYSIS_COMPLETED("ProductArchitectureAnalysisCompleted"),
    PRODUCT_ARCHITECTURE_FINDING_CREATED("ProductArchitectureFindingCreated"),
    PRODUCT_DISTRIBUTED_MONOLITH_ASSESSMENT_COMPLETED("ProductDistributedMonolithAssessmentCompleted"),
    PRODUCT_RECOMMENDATION_GENERATION_STARTED("ProductRecommendationGenerationStarted"),
    PRODUCT_RECOMMENDATION_GENERATED("ProductRecommendationGenerated"),
    PRODUCT_RECOMMENDATION_SUBMITTED_FOR_REVIEW("ProductRecommendationSubmittedForReview"),
    PRODUCT_RECOMMENDATION_STATUS_CHANGED("ProductRecommendationStatusChanged"),
    PRODUCT_DEPENDENCY_COMPOSITION_COMPLETED("ProductDependencyCompositionCompleted"),
    PRODUCT_COMPATIBILITY_EVALUATED("ProductCompatibilityEvaluated"),
    PRODUCT_COMPOSITION_VERSION_CREATED("ProductCompositionVersionCreated");

    private final String eventName;

    ArchitectureEventType(String eventName) {
        this.eventName = eventName;
    }

    public String eventName() {
        return eventName;
    }
}
