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
}
