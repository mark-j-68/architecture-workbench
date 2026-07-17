import type {
  CloseReviewBoardSessionRequest,
  CreateWorkspaceRequest,
  DecideProposedChangeRequest,
  DiscoveryRunResponse,
  DiscoveryRunDetails,
  DiscoveryRunSummary,
  DiscoveryEvidenceView,
  DiscoveryObservationView,
  DiscoveryMetricView,
  DiscoveryDiagnosticView,
  FindingResponse,
  GenerateProjectionRequest,
  GraphResponse,
  OpenReviewBoardSessionRequest,
  ProjectionResponse,
  ProposedChangeResponse,
  RecommendationResponse,
  RecordReviewBoardVoteRequest,
  ReviewBoardSessionResponse,
  RunLocalDiscoveryRequest,
  WorkspaceResponse,
  ProductView,
  ProductCompositionView,
  ProductDependencyCompositionView,
  ProductArchitectureAnalysisView,
  ProductArchitectureRecommendationView,
  ProductRecommendationGenerationView,
} from './architectureApiTypes'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => undefined)
    throw new Error(body?.message ?? `${response.status} ${response.statusText}`)
  }
  return response.json() as Promise<T>
}

function post<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

function query(path: string, filters: Record<string, string | number | undefined>): string {
  const parameters = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => { if (value !== undefined && value !== '') parameters.set(key, String(value)) })
  const encoded = parameters.toString()
  return encoded ? `${path}?${encoded}` : path
}

export const architectureApi = {
  createWorkspace: (body: CreateWorkspaceRequest) => post<WorkspaceResponse>('/api/workspaces', body),
  listWorkspaces: () => request<WorkspaceResponse[]>('/api/workspaces'),
  createProduct: (workspaceId:string, body:{name:string;description:string;actorRef:string}) => post<ProductView>(`/api/workspaces/${workspaceId}/products`,body),
  listProducts: (workspaceId:string) => request<ProductView[]>(`/api/workspaces/${workspaceId}/products`),
  addProductRepository: (workspaceId:string,productId:string,body:unknown) => post<ProductView>(`/api/workspaces/${workspaceId}/products/${productId}/repositories`,body),
  createProductModule: (workspaceId:string,productId:string,body:unknown) => post<ProductView>(`/api/workspaces/${workspaceId}/products/${productId}/modules`,body),
  assignProductRepository: (workspaceId:string,productId:string,moduleId:string,repositoryId:string) => post<ProductView>(`/api/workspaces/${workspaceId}/products/${productId}/modules/${moduleId}/repositories/${repositoryId}?actorRef=ui-user`,{}),
  composeProduct: (workspaceId:string,productId:string) => post<ProductCompositionView>(`/api/workspaces/${workspaceId}/products/${productId}/compose?actorRef=ui-user`,{}),
  composeProductDependencies: (workspaceId:string,productId:string) => post<ProductDependencyCompositionView>(`/api/workspaces/${workspaceId}/products/${productId}/dependencies/compose?actorRef=ui-user`,{}),
  analyseProductArchitecture: (workspaceId:string,productId:string) => post<ProductArchitectureAnalysisView>(`/api/workspaces/${workspaceId}/products/${productId}/analysis?actorRef=ui-user`,{}),
  getProductAnalysisHistory: (workspaceId:string,productId:string) => request<ProductArchitectureAnalysisView[]>(`/api/workspaces/${workspaceId}/products/${productId}/analysis-history`),
  generateProductRecommendations: (workspaceId:string,productId:string) => post<ProductRecommendationGenerationView>(`/api/workspaces/${workspaceId}/products/${productId}/recommendations/generate?actorRef=ui-user`,{}),
  listProductRecommendations: (workspaceId:string,productId:string) => request<ProductArchitectureRecommendationView[]>(`/api/workspaces/${workspaceId}/products/${productId}/recommendations`),
  submitProductRecommendation: (workspaceId:string,productId:string,id:string) => post<ProductArchitectureRecommendationView>(`/api/workspaces/${workspaceId}/products/${productId}/recommendations/${id}/submit-review`,{actorRef:'ui-user',rationale:'Submitted from Product workspace'}),
  createProductProposedChange: (workspaceId:string,productId:string,id:string) => post<{proposedChangeId:string;boundary:string}>(`/api/workspaces/${workspaceId}/products/${productId}/recommendations/${id}/create-proposed-change`,{actorRef:'ui-user',rationale:'Explicitly requested from recommendation'}),
  getWorkspaceGraph: (workspaceId: string) => request<GraphResponse>(`/api/workspaces/${workspaceId}/graph`),
  runLocalDiscovery: (workspaceId: string, body: RunLocalDiscoveryRequest) =>
    post<DiscoveryRunResponse>(`/api/workspaces/${workspaceId}/discovery/local`, body),
  createDiscoveryRun: (workspaceId: string, body: RunLocalDiscoveryRequest) =>
    post<DiscoveryRunDetails>(`/api/workspaces/${workspaceId}/discovery-runs`, body),
  listDiscoveryRuns: (workspaceId: string) => request<DiscoveryRunSummary[]>(`/api/workspaces/${workspaceId}/discovery-runs`),
  getDiscoveryRun: (workspaceId: string, runId: string) => request<DiscoveryRunDetails>(`/api/workspaces/${workspaceId}/discovery-runs/${runId}`),
  listDiscoveryEvidence: (workspaceId: string, runId: string, filters: Record<string, string | number | undefined>) =>
    request<DiscoveryEvidenceView[]>(query(`/api/workspaces/${workspaceId}/discovery-runs/${runId}/evidence`, filters)),
  listDiscoveryObservations: (workspaceId: string, runId: string, filters: Record<string, string | number | undefined> = {}) =>
    request<DiscoveryObservationView[]>(query(`/api/workspaces/${workspaceId}/discovery-runs/${runId}/observations`, filters)),
  listDiscoveryMetrics: (workspaceId: string, runId: string, filters: Record<string, string | number | undefined> = {}) =>
    request<DiscoveryMetricView[]>(query(`/api/workspaces/${workspaceId}/discovery-runs/${runId}/metrics`, filters)),
  listDiscoveryDiagnostics: (workspaceId: string, runId: string, filters: Record<string, string | number | undefined> = {}) =>
    request<DiscoveryDiagnosticView[]>(query(`/api/workspaces/${workspaceId}/discovery-runs/${runId}/diagnostics`, filters)),
  listDiscoveryFindings: (workspaceId: string, runId: string) =>
    request<FindingResponse[]>(`/api/workspaces/${workspaceId}/discovery/runs/${runId}/findings`),
  listDiscoveryRecommendations: (workspaceId: string, runId: string) =>
    request<RecommendationResponse[]>(`/api/workspaces/${workspaceId}/discovery/runs/${runId}/recommendations`),
  listDiscoveryProposedChanges: (workspaceId: string, runId: string) =>
    request<ProposedChangeResponse[]>(`/api/workspaces/${workspaceId}/discovery/runs/${runId}/proposed-changes`),
  openReviewBoardSession: (workspaceId: string, body: OpenReviewBoardSessionRequest) =>
    post<ReviewBoardSessionResponse>(`/api/workspaces/${workspaceId}/review-board/sessions`, body),
  recordReviewBoardVote: (sessionId: string, body: RecordReviewBoardVoteRequest) =>
    post<ReviewBoardSessionResponse>(`/api/review-board/sessions/${sessionId}/votes`, body),
  closeReviewBoardSession: (sessionId: string, body: CloseReviewBoardSessionRequest) =>
    post<ReviewBoardSessionResponse>(`/api/review-board/sessions/${sessionId}/close`, body),
  acceptProposedChange: (proposedChangeId: string, body: DecideProposedChangeRequest) =>
    post<ProposedChangeResponse>(`/api/proposed-changes/${proposedChangeId}/accept`, body),
  rejectProposedChange: (proposedChangeId: string, body: DecideProposedChangeRequest) =>
    post<ProposedChangeResponse>(`/api/proposed-changes/${proposedChangeId}/reject`, body),
  deferProposedChange: (proposedChangeId: string, body: DecideProposedChangeRequest) =>
    post<ProposedChangeResponse>(`/api/proposed-changes/${proposedChangeId}/defer`, body),
  generateProjection: (workspaceId: string, body: GenerateProjectionRequest) =>
    post<ProjectionResponse>(`/api/workspaces/${workspaceId}/projections`, body),
}
