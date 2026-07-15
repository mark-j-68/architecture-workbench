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
