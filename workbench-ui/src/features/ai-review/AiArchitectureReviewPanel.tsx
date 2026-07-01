import { useMemo, useState } from 'react'
import type { AiArchitectureReviewResult, ReviewKind, ReviewerAssessmentView } from './AiArchitectureReviewTypes'
import { sampleReview } from './sampleReview'

export interface AiArchitectureReviewPanelProps {
  latestReview?: AiArchitectureReviewResult
  reviewHistory?: AiArchitectureReviewResult[]
  onRunReview?: (kind: ReviewKind) => Promise<AiArchitectureReviewResult> | AiArchitectureReviewResult
}

const REVIEW_OPTIONS: Array<{ kind: ReviewKind; label: string }> = [
  { kind: 'ARCHITECTURE', label: 'Architecture' },
  { kind: 'DDD_VALIDATION', label: 'DDD Validation' },
  { kind: 'CONSENSUS', label: 'Consensus' },
]

function AssessmentPanel({ title, assessment }: { title: string; assessment: ReviewerAssessmentView }) {
  return (
    <article style={{ border: '1px solid #30363D', borderRadius: 8, padding: 12, background: '#0D1117' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12 }}>
        <h3 style={{ margin: 0, fontSize: 14 }}>{title}</h3>
        <span style={{ color: '#8B949E', fontSize: 12 }}>{Math.round(assessment.confidence * 100)}%</span>
      </div>
      <div style={{ color: '#58A6FF', fontFamily: 'monospace', fontSize: 12, marginTop: 6 }}>{assessment.verdict}</div>
      <List title="Strengths" items={assessment.strengths} />
      <List title="Risks" items={assessment.risks} />
      <List title="Recommendations" items={assessment.recommendations} />
    </article>
  )
}

function List({ title, items }: { title: string; items: string[] }) {
  return (
    <div style={{ marginTop: 10 }}>
      <div style={{ color: '#8B949E', fontSize: 12, marginBottom: 4 }}>{title}</div>
      {items.length === 0 ? (
        <div style={{ color: '#6E7681', fontSize: 12 }}>None recorded</div>
      ) : (
        <ul style={{ margin: 0, paddingLeft: 18, display: 'grid', gap: 4 }}>
          {items.map((item) => <li key={item} style={{ fontSize: 12, lineHeight: 1.4 }}>{item}</li>)}
        </ul>
      )}
    </div>
  )
}

export function AiArchitectureReviewPanel({ latestReview, reviewHistory = [], onRunReview }: AiArchitectureReviewPanelProps) {
  const [selectedKind, setSelectedKind] = useState<ReviewKind>('CONSENSUS')
  const [activeReview, setActiveReview] = useState<AiArchitectureReviewResult>(latestReview ?? sampleReview)
  const [isRunning, setIsRunning] = useState(false)
  const history = useMemo(() => [activeReview, ...reviewHistory.filter((item) => item.reviewId !== activeReview.reviewId)], [activeReview, reviewHistory])

  async function runReview() {
    setIsRunning(true)
    try {
      const result = onRunReview ? await onRunReview(selectedKind) : { ...sampleReview, kind: selectedKind }
      setActiveReview(result)
    } finally {
      setIsRunning(false)
    }
  }

  return (
    <section style={{ display: 'grid', gridTemplateRows: 'auto 1fr', gap: 12, background: '#010409', color: '#E6EDF3', border: '1px solid #30363D', borderRadius: 8, padding: 14 }}>
      <header style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        <h2 style={{ margin: 0, fontSize: 18 }}>AI Architecture Review</h2>
        <select
          value={selectedKind}
          onChange={(event) => setSelectedKind(event.target.value as ReviewKind)}
          style={{ marginLeft: 'auto', background: '#0D1117', color: '#E6EDF3', border: '1px solid #30363D', borderRadius: 6, padding: '7px 9px' }}
        >
          {REVIEW_OPTIONS.map((option) => <option key={option.kind} value={option.kind}>{option.label}</option>)}
        </select>
        <button
          type="button"
          onClick={runReview}
          disabled={isRunning}
          style={{ border: '1px solid #58A6FF', background: isRunning ? '#1F2937' : '#1F3557', color: '#C9D1D9', borderRadius: 6, padding: '8px 12px', cursor: isRunning ? 'wait' : 'pointer' }}
        >
          {isRunning ? 'Running' : 'Run Review'}
        </button>
      </header>

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) minmax(0, 1fr)', gap: 12 }}>
        <AssessmentPanel title="Claude assessment" assessment={activeReview.claudeAssessment} />
        <AssessmentPanel title="OpenAI/Codex assessment" assessment={activeReview.openAiCodexAssessment} />

        <article style={{ border: '1px solid #30363D', borderRadius: 8, padding: 12, background: '#0D1117' }}>
          <h3 style={{ margin: 0, fontSize: 14 }}>Disagreements</h3>
          <List title="Reviewer differences" items={activeReview.disagreements} />
          <h3 style={{ margin: '14px 0 6px', fontSize: 14 }}>Consensus recommendation</h3>
          <p style={{ margin: 0, fontSize: 13, lineHeight: 1.5 }}>{activeReview.consensusRecommendation}</p>
        </article>

        <article style={{ border: '1px solid #30363D', borderRadius: 8, padding: 12, background: '#0D1117' }}>
          <h3 style={{ margin: 0, fontSize: 14 }}>Generated ADR draft</h3>
          <pre style={{ whiteSpace: 'pre-wrap', margin: '10px 0 0', color: '#C9D1D9', fontSize: 12, lineHeight: 1.45, fontFamily: 'ui-monospace, SFMono-Regular, Menlo, monospace' }}>{activeReview.adrDraft}</pre>
        </article>

        <article style={{ gridColumn: '1 / -1', border: '1px solid #30363D', borderRadius: 8, padding: 12, background: '#0D1117' }}>
          <h3 style={{ margin: 0, fontSize: 14 }}>Traceability</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, minmax(0, 1fr))', gap: 10, marginTop: 10, fontSize: 12 }}>
            <span>Activity: {activeReview.auditEnvelope.activityId}</span>
            <span>Envelope hash: {activeReview.auditEnvelope.envelopeHash}</span>
            <span>Payload hash: {activeReview.auditEnvelope.payloadHash}</span>
            <span>Protected payloads: {activeReview.auditEnvelope.protectedPayloads.length}</span>
          </div>
        </article>

        <article style={{ gridColumn: '1 / -1', border: '1px solid #30363D', borderRadius: 8, padding: 12, background: '#0D1117' }}>
          <h3 style={{ margin: 0, fontSize: 14 }}>Review history</h3>
          <div style={{ display: 'grid', gap: 6, marginTop: 10 }}>
            {history.map((review) => (
              <button
                key={review.reviewId}
                type="button"
                onClick={() => setActiveReview(review)}
                style={{ textAlign: 'left', border: '1px solid #30363D', background: review.reviewId === activeReview.reviewId ? '#161B22' : 'transparent', color: '#C9D1D9', borderRadius: 6, padding: 8, cursor: 'pointer' }}
              >
                {review.kind} · {review.reviewId} · {review.consensusRecommendation}
              </button>
            ))}
          </div>
        </article>
      </div>
    </section>
  )
}
