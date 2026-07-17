package com.architectureworkbench.api;
import java.util.List;
import java.util.Optional;
interface ProductRepositoryStore {
    ProductModels.Product save(ProductModels.Product product);
    Optional<ProductModels.Product> find(String workspaceId,String productId);
    List<ProductModels.Product> list(String workspaceId);
    void delete(String workspaceId,String productId);
    void saveComposition(String workspaceId,String productId,ApiDtos.ProductCompositionView composition);
    Optional<ApiDtos.ProductCompositionView> composition(String workspaceId,String productId);
    void saveDependencyComposition(String workspaceId,String productId,ApiDtos.ProductDependencyCompositionView composition);
    Optional<ApiDtos.ProductDependencyCompositionView> dependencyComposition(String workspaceId,String productId);
    List<ApiDtos.ProductDependencyCompositionView> dependencyCompositions(String workspaceId,String productId);
    void saveArchitectureAnalysis(String workspaceId,String productId,ApiDtos.ProductArchitectureAnalysisView analysis);
    Optional<ApiDtos.ProductArchitectureAnalysisView> architectureAnalysis(String workspaceId,String productId);
    List<ApiDtos.ProductArchitectureAnalysisView> architectureAnalyses(String workspaceId,String productId);
    void saveRecommendationGeneration(String workspaceId,String productId,ApiDtos.ProductRecommendationGenerationView generation);
    List<ApiDtos.ProductRecommendationGenerationView> recommendationGenerations(String workspaceId,String productId);
}
