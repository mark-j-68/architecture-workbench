package com.architectureworkbench.api;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
final class InMemoryProductRepositoryStore implements ProductRepositoryStore {
 private final Map<String,ProductModels.Product> products=new ConcurrentHashMap<>(); private final Map<String,ApiDtos.ProductCompositionView> compositions=new ConcurrentHashMap<>();
 private String key(String w,String p){return w+"/"+p;}
 public ProductModels.Product save(ProductModels.Product p){products.put(key(p.workspaceId(),p.id().value()),p);return p;}
 public Optional<ProductModels.Product> find(String w,String p){return Optional.ofNullable(products.get(key(w,p)));}
 public List<ProductModels.Product> list(String w){return products.values().stream().filter(p->p.workspaceId().equals(w)).sorted(Comparator.comparing(p->p.name().value())).toList();}
 public void delete(String w,String p){products.remove(key(w,p));compositions.remove(key(w,p));}
 public void saveComposition(String w,String p,ApiDtos.ProductCompositionView c){compositions.put(key(w,p),c);}
 public Optional<ApiDtos.ProductCompositionView> composition(String w,String p){return Optional.ofNullable(compositions.get(key(w,p)));}
}
