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
