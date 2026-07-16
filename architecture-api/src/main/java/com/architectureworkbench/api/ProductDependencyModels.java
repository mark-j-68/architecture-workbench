package com.architectureworkbench.api;

import java.util.Objects;
import java.util.UUID;
import java.time.Instant;
import java.util.List;

/** Immutable vocabulary for neutral, evidence-backed Product dependencies. */
final class ProductDependencyModels {
    private ProductDependencyModels() {}
    record ProductDependencyId(String value) { ProductDependencyId { value=required(value); } static ProductDependencyId create(){return new ProductDependencyId("dependency-"+UUID.randomUUID());} }
    enum ProductDependencyType { API_CONTRACT,EVENT_CONTRACT,COMMAND_CONTRACT,MESSAGE_CHANNEL,MAVEN_ARTIFACT,SHARED_LIBRARY,SHARED_SCHEMA,SHARED_DATABASE,RELEASE_COORDINATION,DEPLOYMENT_ORDER,ENVIRONMENT_CONFIGURATION,OWNERSHIP_COORDINATION }
    enum ProductDependencyDirection { SOURCE_TO_TARGET,BIDIRECTIONAL,SHARED }
    enum ProductDependencyStatus { OBSERVED,INFERRED,UNRESOLVED }
    enum CompatibilityStatus { COMPATIBLE,INCOMPATIBLE,UNKNOWN,NOT_APPLICABLE }
    record DependencyConfidence(double value,String rationale){DependencyConfidence{if(value<0||value>1)throw new IllegalArgumentException("confidence must be between 0 and 1");rationale=Objects.requireNonNullElse(rationale,"");}}
    record ReleaseStreamId(String value){ReleaseStreamId{value=required(value);}}
    record ReleaseVersion(String value){ReleaseVersion{value=required(value);}}
    record ReleaseStream(ReleaseStreamId id,String name){ReleaseStream{Objects.requireNonNull(id);name=required(name);}}
    record ReleaseArtifact(String identity,ReleaseVersion version){ReleaseArtifact{identity=required(identity);Objects.requireNonNull(version);}}
    record DependencyEvidenceReference(String repositoryId,String discoveryRunId,String evidenceId){DependencyEvidenceReference{repositoryId=required(repositoryId);discoveryRunId=required(discoveryRunId);evidenceId=required(evidenceId);}}
    record ProductDependency(ProductDependencyId id,ProductDependencyType type,ProductDependencyDirection direction,ProductDependencyStatus status,String sourceRepositoryId,String targetRepositoryId,List<DependencyEvidenceReference> evidence,DependencyConfidence confidence){ProductDependency{Objects.requireNonNull(id);Objects.requireNonNull(type);Objects.requireNonNull(direction);Objects.requireNonNull(status);sourceRepositoryId=required(sourceRepositoryId);targetRepositoryId=Objects.requireNonNullElse(targetRepositoryId,"");evidence=List.copyOf(Objects.requireNonNullElse(evidence,List.of()));Objects.requireNonNull(confidence);}}
    record ContractDependency(ProductDependency dependency,String contractId,String producerVersion,String consumerVersion){ContractDependency{Objects.requireNonNull(dependency);contractId=required(contractId);producerVersion=Objects.requireNonNullElse(producerVersion,"");consumerVersion=Objects.requireNonNullElse(consumerVersion,"");}}
    record SharedArtifactDependency(ProductDependency dependency,String coordinates,String sourceVersion,String targetVersion){SharedArtifactDependency{Objects.requireNonNull(dependency);coordinates=required(coordinates);}}
    record ReleaseDependency(ProductDependency dependency,String releaseIdentity){ReleaseDependency{Objects.requireNonNull(dependency);releaseIdentity=required(releaseIdentity);}}
    record DeploymentDependency(ProductDependency dependency,String resourceIdentity){DeploymentDependency{Objects.requireNonNull(dependency);resourceIdentity=required(resourceIdentity);}}
    record OwnershipDependency(ProductDependency dependency,String owner){OwnershipDependency{Objects.requireNonNull(dependency);owner=required(owner);}}
    record CompatibilityRelationship(String identity,CompatibilityStatus status,String sourceVersion,String targetVersion,List<DependencyEvidenceReference> evidence){CompatibilityRelationship{identity=required(identity);Objects.requireNonNull(status);sourceVersion=Objects.requireNonNullElse(sourceVersion,"");targetVersion=Objects.requireNonNullElse(targetVersion,"");evidence=List.copyOf(Objects.requireNonNullElse(evidence,List.of()));}}
    record ReleaseRecord(ReleaseStreamId streamId,ReleaseVersion version,Instant observedAt,List<ReleaseArtifact> artifacts){ReleaseRecord{Objects.requireNonNull(streamId);Objects.requireNonNull(version);observedAt=Objects.requireNonNullElseGet(observedAt,Instant::now);artifacts=List.copyOf(Objects.requireNonNullElse(artifacts,List.of()));}}
    record ReleaseAssociation(String repositoryId,ReleaseStreamId streamId,ReleaseVersion version){ReleaseAssociation{repositoryId=required(repositoryId);Objects.requireNonNull(streamId);Objects.requireNonNull(version);}}
    record ReleaseDependencyEvidence(ReleaseAssociation source,ReleaseAssociation target,List<DependencyEvidenceReference> evidence){ReleaseDependencyEvidence{Objects.requireNonNull(source);Objects.requireNonNull(target);evidence=List.copyOf(Objects.requireNonNullElse(evidence,List.of()));}}
    record ReleaseCompatibilityEvidence(ReleaseAssociation source,ReleaseAssociation target,CompatibilityStatus status){ReleaseCompatibilityEvidence{Objects.requireNonNull(source);Objects.requireNonNull(target);Objects.requireNonNull(status);}}
    private static String required(String value){if(value==null||value.isBlank())throw new IllegalArgumentException("value is required");return value;}
}
