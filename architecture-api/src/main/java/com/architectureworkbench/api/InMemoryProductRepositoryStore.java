package com.architectureworkbench.api;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
final class InMemoryProductRepositoryStore implements ProductRepositoryStore {
 private final Map<String,ProductModels.Product> products=new ConcurrentHashMap<>(); private final Map<String,ApiDtos.ProductCompositionView> compositions=new ConcurrentHashMap<>(); private final Map<String,List<ApiDtos.ProductDependencyCompositionView>> dependencies=new ConcurrentHashMap<>(); private final Map<String,List<ApiDtos.ProductArchitectureAnalysisView>> analyses=new ConcurrentHashMap<>();
 private String key(String w,String p){return w+"/"+p;}
 public ProductModels.Product save(ProductModels.Product p){products.put(key(p.workspaceId(),p.id().value()),p);return p;}
 public Optional<ProductModels.Product> find(String w,String p){return Optional.ofNullable(products.get(key(w,p)));}
 public List<ProductModels.Product> list(String w){return products.values().stream().filter(p->p.workspaceId().equals(w)).sorted(Comparator.comparing(p->p.name().value())).toList();}
 public void delete(String w,String p){products.remove(key(w,p));compositions.remove(key(w,p));}
 public void saveComposition(String w,String p,ApiDtos.ProductCompositionView c){compositions.put(key(w,p),c);}
 public Optional<ApiDtos.ProductCompositionView> composition(String w,String p){return Optional.ofNullable(compositions.get(key(w,p)));}
 public void saveDependencyComposition(String w,String p,ApiDtos.ProductDependencyCompositionView c){dependencies.compute(key(w,p),(k,v)->{var next=new ArrayList<>(v==null?List.<ApiDtos.ProductDependencyCompositionView>of():v);next.add(c);return List.copyOf(next);});}
 public Optional<ApiDtos.ProductDependencyCompositionView> dependencyComposition(String w,String p){var all=dependencyCompositions(w,p);return all.isEmpty()?Optional.empty():Optional.of(all.get(all.size()-1));}
 public List<ApiDtos.ProductDependencyCompositionView> dependencyCompositions(String w,String p){return dependencies.getOrDefault(key(w,p),List.of());}
 public void saveArchitectureAnalysis(String w,String p,ApiDtos.ProductArchitectureAnalysisView a){analyses.compute(key(w,p),(k,v)->{var n=new ArrayList<>(v==null?List.<ApiDtos.ProductArchitectureAnalysisView>of():v);n.add(a);return List.copyOf(n);});}
 public Optional<ApiDtos.ProductArchitectureAnalysisView> architectureAnalysis(String w,String p){var a=architectureAnalyses(w,p);return a.isEmpty()?Optional.empty():Optional.of(a.get(a.size()-1));}
 public List<ApiDtos.ProductArchitectureAnalysisView> architectureAnalyses(String w,String p){return analyses.getOrDefault(key(w,p),List.of());}
}
