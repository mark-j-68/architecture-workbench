export interface CreateWorkspaceRequest {
  name: string
  actorRef: string
}

export interface WorkspaceResponse {
  id: string
  name: string
  graphId: string
}

export interface GraphResponse {
  graphId: string
  elements: ElementResponse[]
  relationships: RelationshipResponse[]
}

export interface ElementResponse {
  id: string
  type: string
  name: string
  description: string
  attributes: Record<string, string>
}

export interface RelationshipResponse {
  id: string
  sourceId: string
  targetId: string
  type: string
  label: string
  attributes: Record<string, string>
}

export interface RunLocalDiscoveryRequest {
  path: string
  actorRef: string
}

export interface DiscoveryRunResponse {
  runId: string
  workspaceId: string
  graphId: string
  startedAt: string
  completedAt: string
  artifactCount: number
  findingCount: number
  recommendationCount: number
  proposedChangeCount: number
}

export interface DiscoveryRunSummary {
  runId: string
  workspaceId: string
  sourceReference: string
  status: string
  startedAt: string
  completedAt: string
  correlationId: string
  causationId: string
  pluginExecutionCount: number
  evidenceCount: number
  observationCount: number
  metricCount: number
  warningCount: number
  failureCount: number
  partialSuccess: boolean
}

export interface DiscoveryConfidenceView { value: number; percentage: number; band: string; rationale: string }
export interface DiscoveryProvenanceView {
  repositoryRelativeFilePath: string; module: string; packageName: string; symbol: string
  lineNumber: number | null; source: string; provenance: string
}
export interface DiscoveryEvidenceView {
  evidenceId: string; type: string; title: string; summary: string; provenance: DiscoveryProvenanceView
  confidence: DiscoveryConfidenceView; classification: string; sourceEvidenceIds: string[]
  derivationSummary: string; unresolvedValue: string; attributes: Record<string, string>
}
export interface DiscoveryObservationView {
  observationId: string; observationType: string; description: string; pluginId: string; module: string
  classification: string; confidence: DiscoveryConfidenceView; derivationSummary: string; supportingEvidenceIds: string[]
}
export interface DiscoveryMetricView {
  metricId: string; name: string; value: number; unit: string; scope: string; module: string; pluginId: string
  derivationSummary: string; confidence: DiscoveryConfidenceView; supportingEvidenceIds: string[]
}
export interface DiscoveryDiagnosticView { diagnosticId: string; pluginId: string; severity: string; message: string; occurredAt: string }
export interface DiscoveryPluginExecution {
  pluginId: string; name: string; category: string; status: string; startedAt: string; completedAt: string
  evidenceCount: number; observationCount: number; metricCount: number; warnings: string[]; errors: string[]
  dependencies: string[]; partialSuccess: boolean
}
export interface DiscoveryRunDetails {
  summary: DiscoveryRunSummary; plugins: DiscoveryPluginExecution[]; evidence: DiscoveryEvidenceView[]
  observations: DiscoveryObservationView[]; metrics: DiscoveryMetricView[]; diagnostics: DiscoveryDiagnosticView[]
  confidenceDistribution: Record<string, number>
}

export interface FindingResponse {
  id: string
  severity: string
  category: string
  description: string
  confidence: number
  evidenceIds: string[]
}

export interface RecommendationResponse {
  id: string
  description: string
  rationale: string
  estimatedImpact: string
  estimatedEffort: string
  confidence: number
  lifecycleStatus: string
  findingIds: string[]
}

export interface ProposedChangeResponse {
  id: string
  type: string
  status: string
  workspaceId: string
  correlationId: string
  recommendationId: string
  findingIds: string[]
  evidenceIds: string[]
  mutation: Record<string, string>
}

export interface ReviewBoardParticipantRequest {
  participantId: string
  name: string
  participantType: string
}

export interface OpenReviewBoardSessionRequest {
  recommendationIds: string[]
  proposedChangeIds: string[]
  participants: ReviewBoardParticipantRequest[]
  actorRef: string
}

export interface ReviewBoardSessionResponse {
  sessionId: string
  workspaceId: string
  correlationId: string
  status: string
  recommendationIds: string[]
  proposedChangeIds: string[]
  participants: ReviewBoardParticipantResponse[]
  votes: ReviewBoardVoteResponse[]
  decision: ReviewBoardDecisionResponse | null
}

export interface ReviewBoardParticipantResponse {
  participantId: string
  name: string
  participantType: string
}

export interface RecordReviewBoardVoteRequest {
  participantId: string
  voteType: string
  rationale: string
}

export interface ReviewBoardVoteResponse {
  voteId: string
  participantId: string
  voteType: string
  rationale: string
  votedAt: string
}

export interface CloseReviewBoardSessionRequest {
  actorRef: string
}

export interface ReviewBoardDecisionResponse {
  decisionType: string
  rationale: string
  conditions: string[]
  decidedAt: string
}

export interface DecideProposedChangeRequest {
  workspaceId: string
  actorRef: string
  rationale: string
}

export interface GenerateProjectionRequest {
  type: string
  actorRef: string
}

export interface ProjectionResponse {
  type: string
  generatedAt: string
  sourceElementRefs: string[]
  sourceRelationshipRefs: string[]
  payload: unknown
}

export interface ProductRepositoryView { repositoryId:string; sourceIdentity:string; sourceReference:string; role:string; moduleId?:string; status:string; discoveryRunIds:string[]; versionMetadata:Record<string,string>; ownershipMetadata:Record<string,string>; addedAt:string }
export interface ProductModuleView { moduleId:string; name:string; description:string; type:string; createdAt:string }
export interface ProductView { productId:string; workspaceId:string; name:string; description:string; status:string; compositionVersion:number; createdAt:string; updatedAt:string; repositories:ProductRepositoryView[]; modules:ProductModuleView[] }
export interface ProductEvidenceItem { productEvidenceId:string; repositoryId:string; repositorySourceIdentity:string; discoveryRunId:string; evidence:DiscoveryEvidenceView }
export interface ProductCompositionMetrics { repositoryCount:number; productModuleCount:number; discoveryRunCount:number; evidenceCount:number; crossRepositoryRelationshipCount:number; sharedContractCount:number; sharedChannelCount:number; unresolvedIdentityCount:number; ownershipMetadataCoverage:number; explicitVersionMetadataCoverage:number; repositoryModuleAssignmentCoverage:number }
export interface ProductCompositionView { productId:string; workspaceId:string; compositionVersion:number; generatedAt:string; contributions:Array<{repositoryId:string;sourceIdentity:string;discoveryRunId:string;evidenceCount:number;observationCount:number;metricCount:number}>; evidence:ProductEvidenceItem[]; observations:unknown[]; repositoryMetrics:unknown[]; identities:Array<{identityId:string;identityType:string;canonicalValue:string;explicitVersion:string;matchType:string;confidence:number;repositoryIds:string[];productEvidenceIds:string[]}>; conflicts:Array<{conflictId:string;rule:string;candidateValue:string;explanation:string;confidence:number;repositoryIds:string[];productEvidenceIds:string[]}>; relationships:Array<{relationshipId:string;type:string;description:string;sourceRepositoryId:string;targetRepositoryId:string;technicalIdentity:string;confidence:number;productEvidenceIds:string[]}>; metrics:ProductCompositionMetrics; diagnostics:Array<{diagnosticId:string;severity:string;message:string}> }
export interface DependencyEvidenceReference { repositoryId:string; discoveryRunId:string; productEvidenceId:string; evidenceId:string; pluginId:string; filePath:string; symbol:string }
export interface ProductDependencyView { dependencyId:string; dependencyType:string; direction:string; status:string; sourceRepositoryId:string; targetRepositoryId:string; technicalIdentity:string; sourceVersion:string; targetVersion:string; compatibilityStatus:string; confidence:number; classification:string; derivationSummary:string; evidence:DependencyEvidenceReference[] }
export interface ContractCompatibilityView { compatibilityId:string; sourceRepositoryId:string; targetRepositoryId:string; contractIdentity:string; producerVersion:string; consumerVersion:string; status:string; explanation:string; confidence:number; evidence:DependencyEvidenceReference[] }
export interface ProductDependencyCompositionView { productId:string; workspaceId:string; compositionVersion:number; generatedAt:string; correlationId:string; inputDiscoveryRunIds:string[]; dependencies:ProductDependencyView[]; compatibility:ContractCompatibilityView[]; releaseRelationships:Array<{relationshipId:string;type:string;sourceRepositoryId:string;targetRepositoryId:string;description:string;sourceVersion:string;targetVersion:string;confidence:number;evidence:DependencyEvidenceReference[]}>; deploymentRelationships:ProductDependencyView[]; ownershipRelationships:Array<{relationshipId:string;type:string;repositoryId:string;relatedRepositoryId:string;owner:string;description:string;confidence:number;evidence:DependencyEvidenceReference[]}>; graph:{nodes:Array<{nodeId:string;nodeType:string;label:string}>;edges:Array<{edgeId:string;sourceNodeId:string;targetNodeId:string;dependencyType:string;label:string;compatibilityStatus:string;confidence:number;evidenceIds:string[]}>}; metrics:{dependencyCount:number;apiDependencyCount:number;eventDependencyCount:number;commandDependencyCount:number;sharedArtifactCount:number;sharedChannelCount:number;explicitCompatibilityCoverage:number;unknownCompatibilityCount:number;explicitIncompatibilityCount:number;releaseAssociationCount:number;coordinatedReleaseEvidenceCount:number;deploymentDependencyCount:number;ownershipConflictCount:number;ownerlessContractCount:number}; diagnostics:Array<{diagnosticId:string;severity:string;status:string;message:string;repositoryIds:string[];evidenceIds:string[]}> }
export interface ProductArchitectureIndicatorView { indicatorId:string; type:string; description:string; confidence:number; dependencyIds:string[]; evidenceIds:string[]; repositoryIds:string[]; moduleIds:string[]; dependencyPath:string[] }
export interface ProductArchitectureFindingView { findingId:string; findingType:string; polarity:'STRENGTH'|'RISK'; title:string; description:string; concern:string; severity:string; confidence:string; confidenceScore:number; indicators:ProductArchitectureIndicatorView[]; supportingObservationIds:string[]; supportingEvidenceIds:string[]; repositoryIds:string[]; moduleIds:string[]; dependencyPaths:string[][]; derivationSummary:string; counterEvidence:string[]; limitations:string[]; compositionVersion:number; generatedAt:string }
export interface ProductArchitectureAnalysisView { analysisId:string; productId:string; workspaceId:string; compositionVersion:number; status:string; startedAt:string; completedAt:string; correlationId:string; findings:ProductArchitectureFindingView[]; assessment:{assessmentId:string;classification:string;confidence:string;confidenceScore:number;evidenceCoverage:number;indicatorSummary:ProductArchitectureIndicatorView[];strengthFindingIds:string[];riskFindingIds:string[];missingEvidence:string[];generatedAt:string}; diagnostics:Array<{diagnosticId:string;severity:string;message:string;missingEvidence:string[]}> }
export interface RecommendationAlternativeView { alternativeId:string;title:string;description:string;impact:string;effort:string;deliveryRisk:string;operationalRisk:string;prerequisites:string[];tradeoffs:Array<{benefit:string;cost:string;risk:string}>;applicability:string;confidence:number }
export interface ProductArchitectureRecommendationView { recommendationId:string;deterministicKey:string;productId:string;workspaceId:string;analysisId:string;compositionVersion:number;title:string;category:string;status:string;priority:string;timeHorizon:string;confidence:string;confidenceScore:number;concerns:string[];supportingFindingIds:string[];supportingIndicatorIds:string[];evidenceIds:string[];repositoryIds:string[];moduleIds:string[];counterEvidence:string[];rationale:string;alternatives:RecommendationAlternativeView[];impact:string;effort:string;risks:string[];preconditions:string[];expectedOutcomes:string[];successMeasures:string[];limitations:string[];recurrenceCount:number;supersedesRecommendationId:string;reviewSessionId:string;proposedChangeId:string;generatedAt:string;updatedAt:string }
export interface ProductRecommendationGenerationView { generationId:string;productId:string;analysisId:string;compositionVersion:number;correlationId:string;generatedAt:string;recommendations:ProductArchitectureRecommendationView[];diagnostics:string[] }
