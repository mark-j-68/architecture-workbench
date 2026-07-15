import { useEffect, useMemo, useState } from 'react'
import { architectureApi } from '../../api/architectureApiClient'
import type { DiscoveryEvidenceView, DiscoveryRunDetails, DiscoveryRunSummary } from '../../api/architectureApiTypes'

type Tab = 'overview' | 'plugins' | 'evidence' | 'observations' | 'metrics' | 'diagnostics'

export interface DiscoveryRunsExplorerProps {
  workspaceId: string
  actorRef: string
  onStatus: (message: string) => void
}

export function DiscoveryRunsExplorer({ workspaceId, actorRef, onStatus }: DiscoveryRunsExplorerProps) {
  const [sourcePath, setSourcePath] = useState('')
  const [runs, setRuns] = useState<DiscoveryRunSummary[]>([])
  const [details, setDetails] = useState<DiscoveryRunDetails | null>(null)
  const [tab, setTab] = useState<Tab>('overview')
  const [busy, setBusy] = useState(false)
  const [search, setSearch] = useState('')
  const [pluginFilter, setPluginFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [classification, setClassification] = useState('')
  const [minimumConfidence, setMinimumConfidence] = useState('0')
  const [moduleFilter, setModuleFilter] = useState('')
  const [packageFilter, setPackageFilter] = useState('')
  const [fileFilter, setFileFilter] = useState('')
  const [selectedEvidence, setSelectedEvidence] = useState<DiscoveryEvidenceView | null>(null)

  useEffect(() => {
    setDetails(null); setSelectedEvidence(null)
    if (workspaceId) refreshRuns().catch(report)
  }, [workspaceId])

  async function refreshRuns() {
    const response = await architectureApi.listDiscoveryRuns(workspaceId); setRuns(response)
  }

  async function startDiscovery() {
    if (!sourcePath) return
    setBusy(true); onStatus('Running deterministic discovery…')
    try {
      const result = await architectureApi.createDiscoveryRun(workspaceId, { path: sourcePath, actorRef })
      setDetails(result); setTab('overview'); await refreshRuns()
      onStatus(`Discovery ${result.summary.status.toLowerCase().replaceAll('_', ' ')} with ${result.summary.evidenceCount} evidence items`)
    } catch (error) { report(error) } finally { setBusy(false) }
  }

  async function openRun(runId: string) {
    setBusy(true)
    try { setDetails(await architectureApi.getDiscoveryRun(workspaceId, runId)); setTab('overview'); setSelectedEvidence(null) }
    catch (error) { report(error) } finally { setBusy(false) }
  }

  async function applyEvidenceFilters() {
    if (!details) return
    const evidence = await architectureApi.listDiscoveryEvidence(workspaceId, details.summary.runId, {
      pluginId: pluginFilter, evidenceType: typeFilter, classification, minimumConfidence,
      module: moduleFilter, package: packageFilter, filePath: fileFilter,
    })
    setDetails({ ...details, evidence }); setSelectedEvidence(null)
  }

  function report(error: unknown) { onStatus(error instanceof Error ? error.message : 'Unable to load discovery data') }

  const visibleEvidence = useMemo(() => {
    const term = search.toLowerCase().trim()
    return details?.evidence.filter((item) => !term || `${item.title} ${item.summary} ${item.provenance.repositoryRelativeFilePath} ${item.provenance.symbol}`.toLowerCase().includes(term)) ?? []
  }, [details, search])

  async function showEvidence(id: string) {
    let item = details?.evidence.find((candidate) => candidate.evidenceId === id) ?? null
    if (!item && details) {
      const complete = await architectureApi.getDiscoveryRun(workspaceId, details.summary.runId)
      setDetails(complete); item = complete.evidence.find((candidate) => candidate.evidenceId === id) ?? null
    }
    setSelectedEvidence(item); setTab('evidence')
  }

  if (!workspaceId) return <section className="panel"><h2>Discovery Runs</h2><p>Select a workspace to inspect deterministic evidence.</p></section>

  return (
    <div className="discovery-explorer">
      <section className="panel discovery-run-header">
        <div>
          <h2>Discovery Runs</h2>
          <p>Repository evidence and deterministic structural observations. Architectural interpretation belongs to Release 0.3.</p>
        </div>
        <div className="discovery-run-form">
          <input aria-label="Repository source path" placeholder="/path/to/local/repository" value={sourcePath} onChange={(event) => setSourcePath(event.target.value)} />
          <button onClick={startDiscovery} disabled={!sourcePath || busy}>Start discovery</button>
        </div>
      </section>

      <div className="discovery-layout">
        <section className="panel run-history">
          <div className="section-title"><h2>Run history</h2><button onClick={() => refreshRuns().catch(report)} disabled={busy}>Refresh</button></div>
          <div className="list compact-list">
            {runs.map((run) => <button key={run.runId} className={details?.summary.runId === run.runId ? 'list-item selected' : 'list-item'} onClick={() => openRun(run.runId)}>
              <span className={`badge ${statusClass(run.status)}`}>{friendly(run.status)}</span>
              <strong>{run.sourceReference}</strong>
              <span>{new Date(run.startedAt).toLocaleString()}</span>
              <span>{run.evidenceCount} evidence · {run.warningCount} warnings</span>
            </button>)}
            {!runs.length && <p className="empty-state">No discovery runs yet.</p>}
          </div>
        </section>

        <section className="panel discovery-details">
          {!details ? <div className="empty-state"><strong>Select or start a discovery run.</strong><p>Every plugin result remains independently inspectable.</p></div> : <>
            <div className="run-summary-line">
              <div><span className={`badge ${statusClass(details.summary.status)}`}>{friendly(details.summary.status)}</span><strong>{details.summary.runId}</strong><p>{details.summary.sourceReference}</p></div>
              <span>{new Date(details.summary.startedAt).toLocaleString()}</span>
            </div>
            <nav className="detail-tabs" aria-label="Discovery run details">
              {(['overview', 'plugins', 'evidence', 'observations', 'metrics', 'diagnostics'] as Tab[]).map((name) =>
                <button key={name} className={tab === name ? 'active' : ''} onClick={() => setTab(name)}>{friendly(name)}</button>)}
            </nav>

            {tab === 'overview' && <Overview details={details} />}
            {tab === 'plugins' && <div className="data-list">{details.plugins.map((plugin) => <article key={plugin.pluginId} className="data-row">
              <div><span className={`badge ${statusClass(plugin.status)}`}>{friendly(plugin.status)}</span><strong>{plugin.name}</strong><p>{plugin.pluginId} · {plugin.category}</p></div>
              <div className="row-counts"><span>{plugin.evidenceCount} evidence</span><span>{plugin.observationCount} observations</span><span>{plugin.metricCount} metrics</span></div>
              <details><summary>Dependencies and diagnostics</summary><p>{plugin.dependencies.join(', ') || 'No dependencies'}</p>{[...plugin.warnings, ...plugin.errors].map((message) => <p key={message}>{message}</p>)}</details>
            </article>)}</div>}
            {tab === 'evidence' && <>
              <div className="evidence-filters">
                <input placeholder="Search evidence" value={search} onChange={(event) => setSearch(event.target.value)} />
                <input placeholder="Plugin id" value={pluginFilter} onChange={(event) => setPluginFilter(event.target.value)} />
                <input placeholder="Evidence type" value={typeFilter} onChange={(event) => setTypeFilter(event.target.value)} />
                <input placeholder="Module" value={moduleFilter} onChange={(event) => setModuleFilter(event.target.value)} />
                <input placeholder="Package" value={packageFilter} onChange={(event) => setPackageFilter(event.target.value)} />
                <input placeholder="File path contains" value={fileFilter} onChange={(event) => setFileFilter(event.target.value)} />
                <select value={classification} onChange={(event) => setClassification(event.target.value)}><option value="">Observed + inferred</option><option value="observed">Observed</option><option value="inferred">Inferred</option></select>
                <select value={minimumConfidence} onChange={(event) => setMinimumConfidence(event.target.value)}><option value="0">Any confidence</option><option value="0.7">70%+</option><option value="0.9">90%+</option><option value="1">100%</option></select>
                <button onClick={() => applyEvidenceFilters().catch(report)}>Apply</button>
              </div>
              <div className="evidence-layout"><div className="data-list evidence-list">{visibleEvidence.map((item) => <button key={item.evidenceId} className={selectedEvidence?.evidenceId === item.evidenceId ? 'data-row evidence-row selected' : 'data-row evidence-row'} onClick={() => setSelectedEvidence(item)}>
                <div><strong>{item.type}</strong><p>{item.summary}</p><code>{location(item)}</code></div>
                <div><span className={`badge ${item.classification}`}>{item.classification}</span><span>{item.confidence.percentage}%</span><p>{item.provenance.source}</p></div>
              </button>)}</div><EvidenceDetail evidence={selectedEvidence} onEvidence={showEvidence} /></div>
            </>}
            {tab === 'observations' && <div className="data-list">{details.observations.map((item) => <article key={item.observationId} className="data-row">
              <div><span className={`badge ${item.classification}`}>{item.classification}</span><strong>{item.observationType}</strong><p>{item.description}</p><small>{item.derivationSummary}</small></div>
              <div><strong>{item.confidence.percentage}%</strong><p>{item.pluginId}</p><div className="evidence-links">{item.supportingEvidenceIds.map((id) => <button key={id} onClick={() => showEvidence(id)}>{id}</button>)}</div></div>
            </article>)}</div>}
            {tab === 'metrics' && <div className="metric-grid">{details.metrics.map((metric) => <article key={metric.metricId} className="metric-card"><span>{metric.scope}</span><strong>{metric.value} <small>{metric.unit}</small></strong><h3>{friendly(metric.name)}</h3><p>{metric.derivationSummary}</p><button onClick={() => metric.supportingEvidenceIds[0] && showEvidence(metric.supportingEvidenceIds[0])}>{metric.supportingEvidenceIds.length} supporting evidence</button></article>)}</div>}
            {tab === 'diagnostics' && <div className="data-list">{details.diagnostics.map((item) => <article key={item.diagnosticId} className="data-row"><span className={`badge ${item.severity.toLowerCase()}`}>{item.severity}</span><div><strong>{item.pluginId}</strong><p>{item.message}</p></div></article>)}{!details.diagnostics.length && <p className="empty-state">No plugin diagnostics.</p>}</div>}
          </>}
        </section>
      </div>
    </div>
  )
}

function Overview({ details }: { details: DiscoveryRunDetails }) {
  const summary = details.summary
  return <div className="overview-stack">
    <dl className="summary-cards">
      <div><dt>Plugins</dt><dd>{summary.pluginExecutionCount}</dd></div><div><dt>Evidence</dt><dd>{summary.evidenceCount}</dd></div>
      <div><dt>Observations</dt><dd>{summary.observationCount}</dd></div><div><dt>Metrics</dt><dd>{summary.metricCount}</dd></div>
      <div><dt>Warnings</dt><dd>{summary.warningCount}</dd></div><div><dt>Failures</dt><dd>{summary.failureCount}</dd></div>
    </dl>
    <div className="overview-grid"><article><h3>Confidence distribution</h3>{Object.entries(details.confidenceDistribution).map(([band, count]) => <p key={band}><strong>{band}</strong> {count}</p>)}</article>
      <article><h3>Plugin outcomes</h3><p>{details.plugins.filter((p) => p.status === 'SUCCEEDED').length} successful</p><p>{details.plugins.filter((p) => p.status === 'PARTIAL_SUCCESS').length} partial</p><p>{details.plugins.filter((p) => p.status === 'FAILED').length} failed</p></article>
      <article><h3>Traceability</h3><p>Correlation: <code>{summary.correlationId}</code></p><p>Completed: {new Date(summary.completedAt).toLocaleString()}</p></article></div>
  </div>
}

function EvidenceDetail({ evidence, onEvidence }: { evidence: DiscoveryEvidenceView | null; onEvidence: (id: string) => void | Promise<void> }) {
  if (!evidence) return <aside className="evidence-detail empty-state">Select evidence to inspect provenance.</aside>
  return <aside className="evidence-detail"><div><span className={`badge ${evidence.classification}`}>{evidence.classification}</span><strong>{evidence.confidence.percentage}% confidence</strong></div>
    <h3>{evidence.type}</h3><p>{evidence.summary}</p><h4>Provenance</h4><code>{location(evidence)}</code><dl><dt>Plugin</dt><dd>{evidence.provenance.source}</dd><dt>Module</dt><dd>{evidence.provenance.module || '—'}</dd><dt>Package</dt><dd>{evidence.provenance.packageName || '—'}</dd><dt>Symbol</dt><dd>{evidence.provenance.symbol || '—'}</dd></dl>
    <h4>Derivation</h4><p>{evidence.derivationSummary}</p>{evidence.unresolvedValue && <p className="warning-text">Unresolved: {evidence.unresolvedValue}</p>}
    <h4>Source evidence</h4><div className="evidence-links">{evidence.sourceEvidenceIds.map((id) => <button key={id} onClick={() => onEvidence(id)}>{id}</button>)}{!evidence.sourceEvidenceIds.length && <span>Direct source evidence</span>}</div>
    <button onClick={() => navigator.clipboard?.writeText(location(evidence))}>Copy location</button></aside>
}

function location(item: DiscoveryEvidenceView) { return `${item.provenance.repositoryRelativeFilePath || 'repository'}${item.provenance.lineNumber ? `:${item.provenance.lineNumber}` : ''}` }
function friendly(value: string) { return value.replaceAll('_', ' ').replaceAll('-', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase()) }
function statusClass(status: string) { const value = status.toLowerCase(); return value.includes('fail') ? 'failed' : value.includes('partial') ? 'partial' : value.includes('complete') || value.includes('succeed') ? 'succeeded' : value }
