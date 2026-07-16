package com.architectureworkbench.audit;
import java.util.Map;
public final class ProductArchitectureEvents {
 private ProductArchitectureEvents(){}
 public record ProductCreated(String productId,String name){public Map<String,String> payload(){return Map.of("productId",productId,"name",name);}}
 public record ProductRepositoryAdded(String productId,String repositoryId){public Map<String,String> payload(){return Map.of("productId",productId,"repositoryId",repositoryId);}}
 public record ProductRepositoryRemoved(String productId,String repositoryId){public Map<String,String> payload(){return Map.of("productId",productId,"repositoryId",repositoryId);}}
 public record ProductModuleCreated(String productId,String moduleId){public Map<String,String> payload(){return Map.of("productId",productId,"moduleId",moduleId);}}
 public record ProductRepositoryAssignedToModule(String productId,String moduleId,String repositoryId){public Map<String,String> payload(){return Map.of("productId",productId,"moduleId",moduleId,"repositoryId",repositoryId);}}
 public record ProductCompositionGenerated(String productId,int evidenceCount,int relationshipCount){public Map<String,String> payload(){return Map.of("productId",productId,"evidenceCount",String.valueOf(evidenceCount),"relationshipCount",String.valueOf(relationshipCount));}}
 public record ProductDependencyCompositionStarted(String productId){public Map<String,String> payload(){return Map.of("productId",productId);}}
 public record ProductDependencyCompositionCompleted(String productId,int dependencyCount){public Map<String,String> payload(){return Map.of("productId",productId,"dependencyCount",String.valueOf(dependencyCount));}}
 public record ProductCompatibilityEvaluated(String productId,int relationshipCount){public Map<String,String> payload(){return Map.of("productId",productId,"relationshipCount",String.valueOf(relationshipCount));}}
 public record ProductCompositionVersionCreated(String productId,long compositionVersion){public Map<String,String> payload(){return Map.of("productId",productId,"compositionVersion",String.valueOf(compositionVersion));}}
}
