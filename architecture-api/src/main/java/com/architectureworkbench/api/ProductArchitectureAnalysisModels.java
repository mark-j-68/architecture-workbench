package com.architectureworkbench.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Immutable Product interpretation vocabulary. AIM-facing DTOs remain adapters at the API boundary. */
final class ProductArchitectureAnalysisModels {
 private ProductArchitectureAnalysisModels() {}
 enum Concern { MODULARITY,RELEASE_INDEPENDENCE,DEPLOYMENT_INDEPENDENCE,CONTRACT_MATURITY,BOUNDED_OWNERSHIP,COMMUNICATION_COMPLEXITY,DATA_OWNERSHIP,PRODUCT_PACKAGING,OPERATIONAL_COUPLING,GOVERNANCE,EVOLVABILITY }
 enum Severity { INFO,LOW,MEDIUM,HIGH,CRITICAL }
 enum Confidence { CONFIRMED,HIGH,MEDIUM,LOW,INSUFFICIENT_EVIDENCE }
 enum Polarity { STRENGTH,RISK }
 enum Status { COMPLETED,PARTIALLY_COMPLETED,FAILED }
 enum Classification { INSUFFICIENT_EVIDENCE,MODULAR,MOSTLY_MODULAR,COUPLED,DISTRIBUTED_MONOLITH_RISK,DISTRIBUTED_MONOLITH_CONFIRMED }
 enum IndicatorType { RELEASE_LOCKSTEP,DEPLOYMENT_LOCKSTEP,CROSS_REPOSITORY_CYCLE,SHARED_DOMAIN_MODEL,CONTRACT_IMMATURITY,SHARED_DATABASE_OR_SCHEMA,SYNCHRONOUS_COUPLING,MESSAGING_COUPLING,CENTRAL_ROUTER_PRESENT,CENTRAL_ROUTER_HIGH_COORDINATION_LOAD,CENTRAL_ROUTER_ESB_DRIFT_RISK,PRODUCT_MODULE_BOUNDARY_MISMATCH,OWNERSHIP_COUPLING,PRODUCT_PACKAGING_COUPLING,INDEPENDENT_VERSIONING,EXPLICIT_CONTRACT_OWNERSHIP,VERSION_COVERAGE,ACYCLIC_DEPENDENCIES,INDEPENDENT_MODULES }
 record AnalysisId(String value) { AnalysisId { required(value); } }
 record ConfidenceValue(Confidence category,double score,String explanation) { ConfidenceValue { Objects.requireNonNull(category);if(score<0||score>1)throw new IllegalArgumentException("score must be between 0 and 1");required(explanation); } }
 record EvidenceSummary(List<String> evidenceIds,List<String> repositories,List<String> modules,String derivation) { EvidenceSummary { evidenceIds=List.copyOf(evidenceIds);repositories=List.copyOf(repositories);modules=List.copyOf(modules);required(derivation); } }
 record Diagnostic(String id,String severity,String message,List<String> missingEvidence) { Diagnostic { required(id);required(severity);required(message);missingEvidence=List.copyOf(missingEvidence); } }
 record Analysis(AnalysisId id,String productId,long compositionVersion,Status status,Instant generatedAt) { Analysis { Objects.requireNonNull(id);required(productId);Objects.requireNonNull(status);Objects.requireNonNull(generatedAt); } }
 private static void required(String value){if(value==null||value.isBlank())throw new IllegalArgumentException("value is required");}
}
