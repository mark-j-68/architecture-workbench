package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.*;
import com.architectureworkbench.audit.*;
import com.architectureworkbench.workspace.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductDependencyAnalysisServiceTest {
 @Test void composesVersionedDependenciesAndRetainsSnapshots() throws Exception {
  var root=Files.createTempDirectory("product-dependencies");String w="workspace-dependencies";Files.createDirectories(root.resolve(w));var store=new FileProductRepositoryStore(root,new ObjectMapper());var audit=new InMemoryAuditSink();
  var p=new ProductModels.Product(ProductModels.ProductId.create(),w,new ProductModels.ProductName("Mortgage"),new ProductModels.ProductDescription(""),ProductModels.ProductStatus.ACTIVE,List.of(repo("orders","2.0","team-a"),repo("loans","1.0","team-b")),List.of(),new ProductModels.ProductCompositionVersion(1),Instant.now(),Instant.now());store.save(p);
  var a=evidence("pe-a",p.repositories().get(0).id().value(),"run-a","event-producer-reference","ApplicationSubmitted","2","producer");var b=evidence("pe-b",p.repositories().get(1).id().value(),"run-b","event-consumer-reference","ApplicationSubmitted","1","consumer");
  store.saveComposition(w,p.id().value(),composition(w,p,List.of(a,b)));
  var service=new ProductDependencyAnalysisService(store,audit);var first=service.compose(w,p.id().value(),"tester");
  assertEquals(1,first.metrics().eventDependencyCount());assertEquals(1,first.metrics().explicitIncompatibilityCount());assertEquals("INCOMPATIBLE",first.compatibility().get(0).status());assertEquals("run-a",first.dependencies().get(0).evidence().get(0).discoveryRunId());assertEquals(1,service.dependencies(w,p.id().value(),"EVENT_CONTRACT",null,"INCOMPATIBLE",.9).size());
  var second=service.compose(w,p.id().value(),"tester");assertEquals(2,second.compositionVersion());assertEquals(2,service.versions(w,p.id().value()).size());
  var reload=new FileProductRepositoryStore(root,new ObjectMapper());assertEquals(2,reload.dependencyCompositions(w,p.id().value()).size());assertTrue(new FileWorkspaceIntegrityService(root).verifyWorkspace(WorkspaceId.of(w)).valid());assertTrue(audit.entries().stream().anyMatch(e->e.action().equals("ProductDependencyCompositionCompleted")));
 }
 @Test void missingLiteralVersionIsUnknownNotIncompatible(){var store=new InMemoryProductRepositoryStore();String w="w";var p=new ProductModels.Product(ProductModels.ProductId.create(),w,new ProductModels.ProductName("P"),new ProductModels.ProductDescription(""),ProductModels.ProductStatus.ACTIVE,List.of(repo("a","",""),repo("b","","")),List.of(),new ProductModels.ProductCompositionVersion(1),Instant.now(),Instant.now());store.save(p);store.saveComposition(w,p.id().value(),composition(w,p,List.of(evidence("a",p.repositories().get(0).id().value(),"a","command-producer-reference","RunCredit","","producer"),evidence("b",p.repositories().get(1).id().value(),"b","command-consumer-reference","RunCredit","1","consumer"))));var result=new ProductDependencyAnalysisService(store,new InMemoryAuditSink()).compose(w,p.id().value(),"x");assertEquals("UNKNOWN",result.compatibility().get(0).status());assertEquals(0,result.metrics().explicitIncompatibilityCount());}
 private ProductModels.ProductRepository repo(String name,String version,String owner){return new ProductModels.ProductRepository(ProductModels.ProductRepositoryId.create(),name,"/"+name,ProductModels.RepositoryRole.SERVICE,null,ProductModels.MembershipStatus.ACTIVE,List.of("run-"+name),version.isBlank()?Map.of():Map.of("version",version),owner.isBlank()?Map.of():Map.of("team",owner),Instant.now());}
 private ProductEvidenceItem evidence(String id,String repo,String run,String type,String contract,String version,String role){Map<String,String>a=new LinkedHashMap<>();a.put("contractId",contract);a.put(type.contains("command")?"commandName":"eventName",contract);if(!version.isBlank())a.put("contractVersion",version);a.put(role,"sample.Symbol");a.put("pluginId","contract.messaging");var e=new DiscoveryEvidenceView("e-"+id,type,contract,contract,new DiscoveryProvenanceView("src/"+contract+".java",".","sample","Symbol",3,"local","contract.messaging"),new DiscoveryConfidenceView(1,100,"HIGH","explicit"),"OBSERVED",List.of(),"explicit",null,a);return new ProductEvidenceItem(id,repo,repo,run,e);}
 private ProductCompositionView composition(String w,ProductModels.Product p,List<ProductEvidenceItem> evidence){return new ProductCompositionView(p.id().value(),w,1,Instant.now(),List.of(),evidence,List.of(),List.of(),List.of(),List.of(),List.of(),new ProductCompositionMetrics(2,0,2,evidence.size(),0,0,0,0,0,0,0),List.of());}
}
