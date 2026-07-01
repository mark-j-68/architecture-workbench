import type { AiArchitectureReviewResult } from './AiArchitectureReviewTypes'

export const sampleReview: AiArchitectureReviewResult = {
  reviewId: 'review-sample',
  kind: 'CONSENSUS',
  claudeAssessment: {
    reviewerId: 'claude-architecture-reviewer',
    provider: 'CLAUDE',
    model: 'claude-reviewer-adapter',
    verdict: 'ACCEPTABLE_WITH_CONTROLS',
    confidence: 0.78,
    strengths: ['Regulatory controls separate immutable envelopes from protected payloads.'],
    risks: ['Consensus judges must remain enabled before automated acceptance.'],
    recommendations: ['Review aggregate invariants against ubiquitous language.'],
    activityId: 'act-sample',
  },
  openAiCodexAssessment: {
    reviewerId: 'openai-codex-architecture-reviewer',
    provider: 'OPENAI_CODEX',
    model: 'codex-reviewer-adapter',
    verdict: 'ACCEPTABLE_WITH_CONTROLS',
    confidence: 0.8,
    strengths: ['Architecture changes are constrained to application services.'],
    risks: ['Endpoint adapters should not bypass validation.'],
    recommendations: ['Keep all model changes behind application services and validation rules.'],
    activityId: 'act-sample',
  },
  disagreements: [
    'CLAUDE unique recommendation: Review aggregate invariants against ubiquitous language.',
    'OPENAI_CODEX unique recommendation: Keep all model changes behind application services and validation rules.',
  ],
  consensusRecommendation: 'Consensus verdict is ACCEPTABLE_WITH_CONTROLS, with reviewer-specific recommendations requiring owner review.',
  adrDraft: `# ADR Draft: AI-Assisted Consensus Review

## Status
Proposed

## Decision
Consensus verdict is ACCEPTABLE_WITH_CONTROLS, with reviewer-specific recommendations requiring owner review.

## Consequences
- Prompt, response, reviewer, tool, and outcome traces are stored via encrypted protected payload references.
- The immutable audit envelope stores only hashes and references.`,
  auditEnvelope: {
    activityId: 'act-sample',
    envelopeHash: 'hash-chain-envelope-sample',
    payloadHash: 'protected-payload-hash-sample',
    protectedPayloads: [
      {
        payloadId: 'pp-sample',
        storageUri: 'memory://protected-payloads/pp-sample',
        encryptionKeyRef: 'kms://architecture-workbench/workspace/pp-sample',
        classification: 'AI_REVIEW_TRACE',
        cryptoShreddable: true,
      },
    ],
  },
}
