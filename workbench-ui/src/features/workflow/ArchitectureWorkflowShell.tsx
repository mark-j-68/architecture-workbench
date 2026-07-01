import { useEffect, useMemo, useState } from 'react'
import { architectureApi } from '../../api/architectureApiClient'
import type {
  DiscoveryRunResponse,
  FindingResponse,
  GraphResponse,
  ProjectionResponse,
  ProposedChangeResponse,
  RecommendationResponse,
  ReviewBoardSessionResponse,
  WorkspaceResponse,
} from '../../api/architectureApiTypes'
import { ArchitectureGraphExplorer } from '../graph/ArchitectureGraphExplorer'
import { graphResponseToViewModel, projectionResponseToViewModel } from './workflowViewModels'
import './workflow.css'

const ACTOR = 'ui-user'

export function ArchitectureWorkflowShell() {
  const [workspaces, setWorkspaces] = useState<WorkspaceResponse[]>([])
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState('')
  const [workspaceName, setWorkspaceName] = useState('Architecture Initiative')
  const [repositoryPath, setRepositoryPath] = useState('')
  const [discoveryRun, setDiscoveryRun] = useState<DiscoveryRunResponse | null>(null)
  const [findings, setFindings] = useState<FindingResponse[]>([])
  const [recommendations, setRecommendations] = useState<RecommendationResponse[]>([])
  const [proposedChanges, setProposedChanges] = useState<ProposedChangeResponse[]>([])
  const [selectedRecommendationId, setSelectedRecommendationId] = useState('')
  const [selectedProposedChangeId, setSelectedProposedChangeId] = useState('')
  const [reviewSession, setReviewSession] = useState<ReviewBoardSessionResponse | null>(null)
  const [graph, setGraph] = useState<GraphResponse | null>(null)
  const [projection, setProjection] = useState<ProjectionResponse | null>(null)
  const [busy, setBusy] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    refreshWorkspaces().catch(showError)
  }, [])

  const selectedWorkspace = workspaces.find((workspace) => workspace.id === selectedWorkspaceId) ?? null
  const graphView = useMemo(() => {
    const fallback = graph ? graphResponseToViewModel(graph) : { nodes: [], edges: [] }
    return projectionResponseToViewModel(projection, fallback)
  }, [graph, projection])

  async function refreshWorkspaces() {
    const response = await architectureApi.listWorkspaces()
    setWorkspaces(response)
    if (!selectedWorkspaceId && response[0]) setSelectedWorkspaceId(response[0].id)
  }

  async function refreshGraph(workspaceId = selectedWorkspaceId) {
    if (!workspaceId) return
    setGraph(await architectureApi.getWorkspaceGraph(workspaceId))
  }

  async function createWorkspace() {
    await run('Creating workspace', async () => {
      const workspace = await architectureApi.createWorkspace({ name: workspaceName, actorRef: ACTOR })
      setWorkspaces((current) => [...current, workspace])
      setSelectedWorkspaceId(workspace.id)
      setDiscoveryRun(null)
      setFindings([])
      setRecommendations([])
      setProposedChanges([])
      setReviewSession(null)
      setProjection(null)
      setGraph(await architectureApi.getWorkspaceGraph(workspace.id))
      setMessage(`Created ${workspace.name}`)
    })
  }

  async function runDiscovery() {
    if (!selectedWorkspaceId || !repositoryPath) return
    await run('Running discovery', async () => {
      const runResponse = await architectureApi.runLocalDiscovery(selectedWorkspaceId, { path: repositoryPath, actorRef: ACTOR })
      const [findingResponse, recommendationResponse, proposedChangeResponse] = await Promise.all([
        architectureApi.listDiscoveryFindings(selectedWorkspaceId, runResponse.runId),
        architectureApi.listDiscoveryRecommendations(selectedWorkspaceId, runResponse.runId),
        architectureApi.listDiscoveryProposedChanges(selectedWorkspaceId, runResponse.runId),
      ])
      setDiscoveryRun(runResponse)
      setFindings(findingResponse)
      setRecommendations(recommendationResponse)
      setProposedChanges(proposedChangeResponse)
      setSelectedRecommendationId(recommendationResponse[0]?.id ?? '')
      setSelectedProposedChangeId(proposedChangeResponse[0]?.id ?? '')
      setReviewSession(null)
      setMessage(`Discovery found ${runResponse.findingCount} findings`)
    })
  }

  async function openReviewBoardSession() {
    if (!selectedWorkspaceId || !selectedRecommendationId || !selectedProposedChangeId) return
    await run('Opening Review Board', async () => {
      const session = await architectureApi.openReviewBoardSession(selectedWorkspaceId, {
        recommendationIds: [selectedRecommendationId],
        proposedChangeIds: [selectedProposedChangeId],
        participants: [
          { participantId: 'architect', name: 'Lead Architect', participantType: 'HUMAN_ARCHITECT' },
          { participantId: 'ddd', name: 'DDD Reviewer', participantType: 'DDD_REVIEWER' },
        ],
        actorRef: ACTOR,
      })
      setReviewSession(session)
      setMessage('Review Board session opened')
    })
  }

  async function vote(participantId: string, voteType: string, rationale: string) {
    if (!reviewSession) return
    await run('Recording vote', async () => {
      const session = await architectureApi.recordReviewBoardVote(reviewSession.sessionId, { participantId, voteType, rationale })
      setReviewSession(session)
    })
  }

  async function closeReviewBoardSession() {
    if (!reviewSession) return
    await run('Closing Review Board', async () => {
      const session = await architectureApi.closeReviewBoardSession(reviewSession.sessionId, { actorRef: ACTOR })
      setReviewSession(session)
      setMessage(session.decision ? `Decision: ${session.decision.decisionType}` : 'Review Board closed')
    })
  }

  async function decideProposedChange(action: 'accept' | 'reject' | 'defer', changeId = selectedProposedChangeId) {
    if (!selectedWorkspaceId || !changeId) return
    await run(`${action} proposed change`, async () => {
      const request = { workspaceId: selectedWorkspaceId, actorRef: ACTOR, rationale: `${action} from UI workflow` }
      const updated = action === 'accept'
        ? await architectureApi.acceptProposedChange(changeId, request)
        : action === 'reject'
          ? await architectureApi.rejectProposedChange(changeId, request)
          : await architectureApi.deferProposedChange(changeId, request)
      setProposedChanges((current) => current.map((change) => change.id === updated.id ? updated : change))
      await refreshGraph()
      setProjection(null)
      setMessage(`Proposed change ${updated.status.toLowerCase()}`)
    })
  }

  async function generateProjection() {
    if (!selectedWorkspaceId) return
    await run('Generating projection', async () => {
      setProjection(await architectureApi.generateProjection(selectedWorkspaceId, { type: 'REACT_FLOW', actorRef: ACTOR }))
      setMessage('React Flow projection generated')
    })
  }

  async function run(label: string, action: () => Promise<void>) {
    setBusy(label)
    setMessage('')
    try {
      await action()
    } catch (error) {
      showError(error)
    } finally {
      setBusy('')
    }
  }

  function showError(error: unknown) {
    setMessage(error instanceof Error ? error.message : 'Unexpected UI error')
  }

  return (
    <div className="workflow-shell">
      <header className="app-header">
        <div>
          <h1>Architecture Workbench</h1>
          <p>Architecture OS workflow adapter</p>
        </div>
        <div className="status-strip">
          <span>{busy || message || 'Ready'}</span>
        </div>
      </header>

      <div className="workspace-layout">
        <aside className="sidebar">
          <section className="panel">
            <h2>Workspaces</h2>
            <div className="form-row">
              <input value={workspaceName} onChange={(event) => setWorkspaceName(event.target.value)} />
              <button onClick={createWorkspace} disabled={Boolean(busy)}>Create</button>
            </div>
            <div className="list compact-list">
              {workspaces.map((workspace) => (
                <button
                  key={workspace.id}
                  className={workspace.id === selectedWorkspaceId ? 'list-item selected' : 'list-item'}
                  onClick={() => {
                    setSelectedWorkspaceId(workspace.id)
                    setDiscoveryRun(null)
                    setFindings([])
                    setRecommendations([])
                    setProposedChanges([])
                    setReviewSession(null)
                    setProjection(null)
                    refreshGraph(workspace.id).catch(showError)
                  }}
                >
                  <strong>{workspace.name}</strong>
                  <span>{workspace.graphId}</span>
                </button>
              ))}
            </div>
          </section>

          <section className="panel">
            <h2>Run Discovery</h2>
            <input
              placeholder="/path/to/local/repository"
              value={repositoryPath}
              onChange={(event) => setRepositoryPath(event.target.value)}
            />
            <button onClick={runDiscovery} disabled={!selectedWorkspaceId || !repositoryPath || Boolean(busy)}>Run Local Discovery</button>
            {discoveryRun && (
              <dl className="metrics">
                <div><dt>Artifacts</dt><dd>{discoveryRun.artifactCount}</dd></div>
                <div><dt>Findings</dt><dd>{discoveryRun.findingCount}</dd></div>
                <div><dt>Proposals</dt><dd>{discoveryRun.proposedChangeCount}</dd></div>
              </dl>
            )}
          </section>
        </aside>

        <main className="content">
          <section className="workspace-detail">
            <div>
              <h2>{selectedWorkspace?.name ?? 'No workspace selected'}</h2>
              <p>{selectedWorkspace?.graphId ?? 'Create or select a workspace to begin.'}</p>
            </div>
            <div className="toolbar">
              <button onClick={() => refreshGraph()} disabled={!selectedWorkspaceId || Boolean(busy)}>Refresh Graph</button>
              <button onClick={generateProjection} disabled={!selectedWorkspaceId || Boolean(busy)}>Generate Projection</button>
            </div>
          </section>

          <div className="results-grid">
            <section className="panel">
              <h2>Discovery Results</h2>
              <div className="scroll-list">
                {findings.map((finding) => (
                  <article key={finding.id} className="result-row">
                    <span className={`badge ${finding.severity.toLowerCase()}`}>{finding.severity}</span>
                    <strong>{finding.category}</strong>
                    <p>{finding.description}</p>
                  </article>
                ))}
              </div>
            </section>

            <section className="panel">
              <h2>Recommendation Candidates</h2>
              <div className="scroll-list">
                {recommendations.map((recommendation) => (
                  <button
                    key={recommendation.id}
                    className={recommendation.id === selectedRecommendationId ? 'result-row selectable selected' : 'result-row selectable'}
                    onClick={() => setSelectedRecommendationId(recommendation.id)}
                  >
                    <strong>{recommendation.description}</strong>
                    <p>{recommendation.rationale}</p>
                    <span>{Math.round(recommendation.confidence * 100)}% confidence</span>
                  </button>
                ))}
              </div>
            </section>

            <section className="panel">
              <h2>Proposed Changes</h2>
              <div className="scroll-list">
                {proposedChanges.map((change) => (
                  <article key={change.id} className={change.id === selectedProposedChangeId ? 'result-row selected' : 'result-row'}>
                    <button className="text-button" onClick={() => setSelectedProposedChangeId(change.id)}>{change.type}</button>
                    <span className={`badge ${change.status.toLowerCase()}`}>{change.status}</span>
                    <p>{change.mutation.name ?? change.mutation.relationshipType ?? change.id}</p>
                    <div className="inline-actions">
                      <button onClick={() => decideProposedChange('accept', change.id)} disabled={change.status !== 'PROPOSED' || Boolean(busy)}>Accept</button>
                      <button onClick={() => decideProposedChange('reject', change.id)} disabled={change.status !== 'PROPOSED' || Boolean(busy)}>Reject</button>
                      <button onClick={() => decideProposedChange('defer', change.id)} disabled={change.status !== 'PROPOSED' || Boolean(busy)}>Defer</button>
                    </div>
                  </article>
                ))}
              </div>
            </section>

            <section className="panel">
              <h2>Review Board Session</h2>
              <button onClick={openReviewBoardSession} disabled={!selectedRecommendationId || !selectedProposedChangeId || Boolean(busy)}>Open Session</button>
              {reviewSession && (
                <div className="review-board">
                  <p><strong>{reviewSession.status}</strong> {reviewSession.sessionId}</p>
                  <div className="inline-actions">
                    <button onClick={() => vote('architect', 'APPROVE', 'Approved from UI workflow')} disabled={reviewSession.status !== 'OPEN'}>Architect Approve</button>
                    <button onClick={() => vote('ddd', 'APPROVE', 'DDD reviewer approved from UI workflow')} disabled={reviewSession.status !== 'OPEN'}>DDD Approve</button>
                    <button onClick={() => vote('ddd', 'REQUEST_MORE_EVIDENCE', 'Need more evidence')} disabled={reviewSession.status !== 'OPEN'}>More Evidence</button>
                    <button onClick={closeReviewBoardSession} disabled={reviewSession.status !== 'OPEN'}>Close</button>
                  </div>
                  {reviewSession.decision && (
                    <article className="decision">
                      <strong>{reviewSession.decision.decisionType}</strong>
                      <p>{reviewSession.decision.rationale}</p>
                    </article>
                  )}
                </div>
              )}
            </section>
          </div>

          <section className="panel graph-panel">
            <h2>Architecture Graph Projection Viewer</h2>
            <ArchitectureGraphExplorer graph={graphView} />
          </section>
        </main>
      </div>
    </div>
  )
}
