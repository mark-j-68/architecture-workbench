package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.*;
import com.architectureworkbench.audit.InMemoryAuditSink;
import com.architectureworkbench.workspace.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductArchitectureAnalysisServiceTest {
 @Test void aggregatesLockstepCycleAndCounterEvidenceIntoExplainableAssessment() throws Exception {
  var root=Files.createTempDirectory("product-analysis");String w="workspace-analysis";Files.createDirectories(root.resolve(w));var store=new FileProductRepositoryStore(root,new ObjectMapper());var audit=new InMemoryAuditSink();var p=product(w,true);store.save(p);store.saveComposition(w,p.id().value(),composition(w,p));
  String a=p.repositories().get(0).id().value(),b=p.repositories().get(1).id().value();var refs=List.of(ref(a,"run-a","event-a"));
  var ab=dependency("ab","EVENT_CONTRACT",a,b,"ApplicationSubmitted","UNKNOWN",refs);var ba=dependency("ba","API_CONTRACT",b,a,"submitApplication","COMPATIBLE",List.of(ref(b,"run-b","api-b")));
  var dep=new ProductDependencyCompositionView(p.id().value(),w,4,Instant.now(),"correlation",List.of("run-a","run-b"),List.of(ab,ba),List.of(new ContractCompatibilityView("compat",a,b,"ApplicationSubmitted","2","1","INCOMPATIBLE","literal conflict",1,refs)),List.of(new ReleaseRelationshipView("release","RELEASE_BUNDLE",a,b,"Explicit bundle","4.7","4.7",1,List.of())),List.of(new ProductDependencyView("deploy","DEPLOYMENT_ORDER","SOURCE_TO_TARGET","OBSERVED",a,b,"orders-before-loans","","","NOT_APPLICABLE",1,"OBSERVED","Explicit deployment order",List.of())),List.of(),new ProductDependencyGraph(List.of(),List.of()),new ProductDependencyMetrics(3,1,1,0,0,0,50,1,1,1,1,1,0,0),List.of());store.saveDependencyComposition(w,p.id().value(),dep);
  var result=new ProductArchitectureAnalysisService(store,audit).analyse(w,p.id().value(),"tester");
  assertEquals("DISTRIBUTED_MONOLITH_CONFIRMED",result.assessment().classification());assertTrue(result.findings().stream().anyMatch(f->f.findingType().equals("CROSS_REPOSITORY_CYCLE")&&!f.dependencyPaths().isEmpty()));assertTrue(result.findings().stream().allMatch(f->!f.derivationSummary().isBlank()));assertTrue(result.findings().stream().anyMatch(f->f.polarity().equals("STRENGTH")));
  var reload=new FileProductRepositoryStore(root,new ObjectMapper());assertEquals(result.analysisId(),reload.architectureAnalysis(w,p.id().value()).orElseThrow().analysisId());assertTrue(new FileWorkspaceIntegrityService(root).verifyWorkspace(WorkspaceId.of(w)).valid());assertTrue(audit.entries().stream().anyMatch(e->e.action().equals("ProductDistributedMonolithAssessmentCompleted")));
 }
 @Test void incompleteSingleRepositoryProductIsInsufficientAndHistoryIsRetained(){var store=new InMemoryProductRepositoryStore();String w="w";var p=product(w,false);store.save(p);store.saveComposition(w,p.id().value(),composition(w,p));store.saveDependencyComposition(w,p.id().value(),emptyDependency(w,p));var service=new ProductArchitectureAnalysisService(store,new InMemoryAuditSink());var one=service.analyse(w,p.id().value(),"x");var two=service.analyse(w,p.id().value(),"x");assertEquals("INSUFFICIENT_EVIDENCE",one.assessment().classification());assertFalse(one.diagnostics().isEmpty());assertEquals(2,service.history(w,p.id().value()).size());assertEquals(two.analysisId(),service.latest(w,p.id().value()).analysisId());}
 private ProductModels.Product product(String w,boolean multi){var repos=new ArrayList<ProductModels.ProductRepository>();repos.add(repo("orders",multi));if(multi)repos.add(repo("loans",true));return new ProductModels.Product(ProductModels.ProductId.create(),w,new ProductModels.ProductName("Mortgage"),new ProductModels.ProductDescription(""),ProductModels.ProductStatus.ACTIVE,repos,List.of(),new ProductModels.ProductCompositionVersion(1),Instant.now(),Instant.now());}
 private ProductModels.ProductRepository repo(String name,boolean run){return new ProductModels.ProductRepository(ProductModels.ProductRepositoryId.create(),name,"/"+name,ProductModels.RepositoryRole.SERVICE,null,ProductModels.MembershipStatus.ACTIVE,run?List.of("run-"+name):List.of(),Map.of("version","1.0"),Map.of("team",name),Instant.now());}
 private ProductCompositionView composition(String w,ProductModels.Product p){return new ProductCompositionView(p.id().value(),w,1,Instant.now(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),new ProductCompositionMetrics(p.repositories().size(),0,p.repositories().size(),1,0,0,0,0,100,100,0),List.of());}
 private ProductDependencyCompositionView emptyDependency(String w,ProductModels.Product p){return new ProductDependencyCompositionView(p.id().value(),w,1,Instant.now(),"c",List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),new ProductDependencyGraph(List.of(),List.of()),new ProductDependencyMetrics(0,0,0,0,0,0,0,0,0,0,0,0,0,0),List.of());}
 private ProductDependencyView dependency(String id,String type,String a,String b,String identity,String compatibility,List<DependencyEvidenceReference> refs){return new ProductDependencyView(id,type,"SOURCE_TO_TARGET","OBSERVED",a,b,identity,"1","1",compatibility,1,"OBSERVED","explicit",refs);}
 private DependencyEvidenceReference ref(String repo,String run,String evidence){return new DependencyEvidenceReference(repo,run,"p-"+evidence,evidence,"plugin","src/App.java","App");}
}
