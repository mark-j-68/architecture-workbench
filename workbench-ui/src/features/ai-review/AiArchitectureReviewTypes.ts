export type ReviewKind = 'ARCHITECTURE' | 'DDD_VALIDATION' | 'CONSENSUS'

export interface ReviewerAssessmentView {
  reviewerId: string
  provider: string
  model: string
  verdict: string
  confidence: number
  strengths: string[]
  risks: string[]
  recommendations: string[]
  activityId?: string
}

export interface AiArchitectureReviewResult {
  reviewId: string
  kind: ReviewKind
  claudeAssessment: ReviewerAssessmentView
  openAiCodexAssessment: ReviewerAssessmentView
  disagreements: string[]
  consensusRecommendation: string
  adrDraft: string
  auditEnvelope: {
    activityId: string
    envelopeHash: string
    payloadHash: string
    protectedPayloads: Array<{
      payloadId: string
      storageUri: string
      encryptionKeyRef: string
      classification: string
      cryptoShreddable: boolean
    }>
  }
}
