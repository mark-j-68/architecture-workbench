package com.architectureworkbench.api;

import com.architectureworkbench.api.ApiDtos.*;
import com.architectureworkbench.audit.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductArchitectureRecommendationServiceTest {
 @Test void generatesAlternativesDeduplicatesHistoryAndNeverMutatesProduct(){
  var store=new InMemoryProductRepositoryStore();var audit=new InMemoryAuditSink();String w="w";var product=product(w);store.save(product);store.saveArchitectureAnalysis(w,product.id().value(),analysis(w,product.id().value(),"CROSS_REPOSITORY_CYCLE","RISK",.92,75));
  var service=new ProductArchitectureRecommendationService(store,audit);var before=store.find(w,product.id().value()).orElseThrow();var first=service.generate(w,product.id().value(),"tester");var second=service.generate(w,product.id().value(),"tester");var recommendation=second.recommendations().get(0);
  assertEquals("DEPENDENCY_CYCLE_REDUCTION",recommendation.category());assertTrue(recommendation.alternatives().size()>=4);assertEquals(2,recommendation.recurrenceCount());assertFalse(recommendation.supersedesRecommendationId().isBlank());assertEquals(before,store.find(w,product.id().value()).orElseThrow());
  var reviewed=service.submitReview(w,product.id().value(),recommendation.recommendationId(),new RecommendationActionRequest("tester","review"));assertEquals("UNDER_REVIEW",reviewed.status());assertFalse(reviewed.reviewSessionId().isBlank());
  var proposal=service.createProposedChange(w,product.id().value(),recommendation.recommendationId(),new RecommendationActionRequest("tester","explicit"));assertTrue(proposal.boundary().contains("canonical"));assertEquals(before,store.find(w,product.id().value()).orElseThrow());assertTrue(audit.entries().stream().anyMatch(e->e.action().equals("ProductRecommendationGenerated")));
 }
 @Test void partialEvidenceProducesFurtherDiscoveryAndStrengthProducesNoChange(){var store=new InMemoryProductRepositoryStore();var p=product("w");store.save(p);store.saveArchitectureAnalysis("w",p.id().value(),analysis("w",p.id().value(),"ACYCLIC_DEPENDENCIES","STRENGTH",.8,40));var result=new ProductArchitectureRecommendationService(store,new InMemoryAuditSink()).generate("w",p.id().value(),"x");assertTrue(result.recommendations().stream().anyMatch(x->x.category().equals("NO_CHANGE_REQUIRED")));assertTrue(result.recommendations().stream().anyMatch(x->x.category().equals("FURTHER_DISCOVERY")));}
 private ProductModels.Product product(String w){return new ProductModels.Product(ProductModels.ProductId.create(),w,new ProductModels.ProductName("Product"),new ProductModels.ProductDescription(""),ProductModels.ProductStatus.ACTIVE,List.of(),List.of(),new ProductModels.ProductCompositionVersion(1),Instant.now(),Instant.now());}
 private ProductArchitectureAnalysisView analysis(String w,String p,String type,String polarity,double confidence,int coverage){var indicator=new ProductArchitectureIndicatorView("i",type,"explicit",confidence,List.of("dep"),List.of("evidence"),List.of("a","b"),List.of(),List.of("a","b","a"));var finding=new ProductArchitectureFindingView("finding",type,polarity,"Finding", "Evidence-backed finding","MODULARITY","HIGH","HIGH",confidence,List.of(indicator),List.of("observation"),List.of("evidence"),List.of("a","b"),List.of(),List.of(List.of("a","b","a")),"deterministic derivation",List.of("Mitigating evidence"),List.of("Delivery history is incomplete"),1,Instant.now());var assessment=new ProductDistributedMonolithAssessment("assessment",p,1,"COMPLETED","COUPLED","MEDIUM",.7,coverage,List.of(indicator),polarity.equals("STRENGTH")?List.of("finding"):List.of(),polarity.equals("RISK")?List.of("finding"):List.of(),List.of(),List.of(),Instant.now());return new ProductArchitectureAnalysisView("analysis",p,w,1,"COMPLETED",Instant.now(),Instant.now(),"correlation",List.of(finding),assessment,List.of());}
}
