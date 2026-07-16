package com.architectureworkbench.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class ProductModels {
    private ProductModels() {}

    record ProductId(String value) { ProductId { value = required(value, "productId"); } static ProductId create() { return new ProductId("product-" + UUID.randomUUID()); } }
    record ProductName(String value) { ProductName { value = required(value, "name"); } }
    record ProductDescription(String value) { ProductDescription { value = Objects.requireNonNullElse(value, ""); } }
    enum ProductStatus { ACTIVE, ARCHIVED }
    record ProductRepositoryId(String value) { ProductRepositoryId { value = required(value, "repositoryId"); } static ProductRepositoryId create() { return new ProductRepositoryId("repository-" + UUID.randomUUID()); } }
    enum RepositoryRole { APPLICATION, SERVICE, LIBRARY, FRONTEND, INFRASTRUCTURE, CONTRACTS, UNKNOWN }
    enum MembershipStatus { ACTIVE, REMOVED }
    record ProductMembership(ProductId productId, ProductRepositoryId repositoryId, MembershipStatus status) {
        ProductMembership { Objects.requireNonNull(productId); Objects.requireNonNull(repositoryId); status=Objects.requireNonNullElse(status,MembershipStatus.ACTIVE); }
    }
    record ProductModuleId(String value) { ProductModuleId { value = required(value, "moduleId"); } static ProductModuleId create() { return new ProductModuleId("module-" + UUID.randomUUID()); } }
    enum ProductModuleType { BUSINESS, APPLICATION, SERVICE, LIBRARY, FRONTEND, ROUTER, OTHER }
    record ProductCompositionVersion(long value) { ProductCompositionVersion { if (value < 0) throw new IllegalArgumentException("composition version cannot be negative"); } ProductCompositionVersion next() { return new ProductCompositionVersion(value + 1); } }

    record ProductRepository(ProductRepositoryId id, String sourceIdentity, String sourceReference, RepositoryRole role,
                             ProductModuleId moduleId, MembershipStatus status, List<String> discoveryRunIds,
                             Map<String,String> versionMetadata, Map<String,String> ownershipMetadata, Instant addedAt) {
        ProductRepository { Objects.requireNonNull(id); sourceIdentity = required(sourceIdentity,"sourceIdentity"); sourceReference = required(sourceReference,"sourceReference"); role = Objects.requireNonNullElse(role, RepositoryRole.UNKNOWN); status = Objects.requireNonNullElse(status, MembershipStatus.ACTIVE); discoveryRunIds = List.copyOf(Objects.requireNonNullElse(discoveryRunIds,List.of())); versionMetadata = Map.copyOf(Objects.requireNonNullElse(versionMetadata,Map.of())); ownershipMetadata = Map.copyOf(Objects.requireNonNullElse(ownershipMetadata,Map.of())); addedAt = Objects.requireNonNullElseGet(addedAt,Instant::now); }
        ProductRepository withRun(String runId) { var ids = new java.util.ArrayList<>(discoveryRunIds); if (!ids.contains(runId)) ids.add(runId); return new ProductRepository(id,sourceIdentity,sourceReference,role,moduleId,status,ids,versionMetadata,ownershipMetadata,addedAt); }
        ProductRepository inModule(ProductModuleId module) { return new ProductRepository(id,sourceIdentity,sourceReference,role,module,status,discoveryRunIds,versionMetadata,ownershipMetadata,addedAt); }
    }
    record ProductModule(ProductModuleId id, String name, String description, ProductModuleType type, Instant createdAt) {
        ProductModule { Objects.requireNonNull(id); name=required(name,"module name"); description=Objects.requireNonNullElse(description,""); type=Objects.requireNonNullElse(type,ProductModuleType.OTHER); createdAt=Objects.requireNonNullElseGet(createdAt,Instant::now); }
    }
    record Product(ProductId id, String workspaceId, ProductName name, ProductDescription description, ProductStatus status,
                   List<ProductRepository> repositories, List<ProductModule> modules, ProductCompositionVersion compositionVersion,
                   Instant createdAt, Instant updatedAt) {
        Product { Objects.requireNonNull(id); workspaceId=required(workspaceId,"workspaceId"); Objects.requireNonNull(name); Objects.requireNonNull(description); status=Objects.requireNonNullElse(status,ProductStatus.ACTIVE); repositories=List.copyOf(Objects.requireNonNullElse(repositories,List.of())); modules=List.copyOf(Objects.requireNonNullElse(modules,List.of())); compositionVersion=Objects.requireNonNullElse(compositionVersion,new ProductCompositionVersion(0)); createdAt=Objects.requireNonNullElseGet(createdAt,Instant::now); updatedAt=Objects.requireNonNullElse(updatedAt,createdAt); }
        Product changed(List<ProductRepository> repos,List<ProductModule> mods) { return new Product(id,workspaceId,name,description,status,repos,mods,compositionVersion.next(),createdAt,Instant.now()); }
        Product withMetadata(ProductName nextName,ProductDescription nextDescription,ProductStatus nextStatus) { return new Product(id,workspaceId,nextName,nextDescription,nextStatus,repositories,modules,compositionVersion.next(),createdAt,Instant.now()); }
    }
    private static String required(String value,String field) { if(value==null||value.isBlank()) throw new IllegalArgumentException(field+" is required."); return value; }
}
