import type { ArchitectureGraphViewModel } from './ArchitectureGraphTypes'

export const sampleMortgageGraph: ArchitectureGraphViewModel = {
  nodes: [
    { id: 'ctx-mortgage', type: 'BOUNDED_CONTEXT', label: 'Mortgage Origination' },
    { id: 'agg-application', type: 'AGGREGATE', label: 'MortgageApplication' },
    { id: 'cmd-submit', type: 'COMMAND', label: 'SubmitMortgageApplication' },
    { id: 'evt-submitted', type: 'DOMAIN_EVENT', label: 'MortgageApplicationSubmitted' },
    { id: 'policy-affordability', type: 'POLICY', label: 'AssessAffordability' },
    { id: 'rm-case-summary', type: 'READ_MODEL', label: 'CaseSummary' },
    { id: 'svc-pos', type: 'SERVICE', label: 'pos-service' },
    { id: 'bus-eventbridge', type: 'DEPLOYMENT_RESOURCE', label: 'EventBridge Bus' },
    { id: 'judge-claude', type: 'AI_JUDGE', label: 'Claude Judge' },
    { id: 'judge-openai', type: 'AI_JUDGE', label: 'OpenAI Judge' },
    { id: 'audit-log', type: 'AUDIT_LOG', label: 'Immutable Activity Log' },
  ],
  edges: [
    { sourceId: 'ctx-mortgage', targetId: 'agg-application', relationship: 'contains' },
    { sourceId: 'cmd-submit', targetId: 'agg-application', relationship: 'targets' },
    { sourceId: 'agg-application', targetId: 'evt-submitted', relationship: 'emits' },
    { sourceId: 'evt-submitted', targetId: 'policy-affordability', relationship: 'triggers' },
    { sourceId: 'evt-submitted', targetId: 'rm-case-summary', relationship: 'populates' },
    { sourceId: 'svc-pos', targetId: 'agg-application', relationship: 'owns' },
    { sourceId: 'evt-submitted', targetId: 'bus-eventbridge', relationship: 'publishes via' },
    { sourceId: 'judge-claude', targetId: 'audit-log', relationship: 'records assessment' },
    { sourceId: 'judge-openai', targetId: 'audit-log', relationship: 'records assessment' },
  ],
}
