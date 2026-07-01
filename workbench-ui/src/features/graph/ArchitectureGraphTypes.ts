export type ArchitectureNodeType =
  | 'BOUNDED_CONTEXT'
  | 'AGGREGATE'
  | 'COMMAND'
  | 'DOMAIN_EVENT'
  | 'POLICY'
  | 'READ_MODEL'
  | 'SERVICE'
  | 'EXTERNAL_SYSTEM'
  | 'DEPLOYMENT_RESOURCE'
  | 'AUDIT_LOG'
  | 'AI_JUDGE'

export interface ArchitectureGraphNode {
  id: string
  type: ArchitectureNodeType | string
  label: string
  description?: string
  metadata?: Record<string, string>
}

export interface ArchitectureGraphEdge {
  id?: string
  sourceId: string
  targetId: string
  relationship: string
  label?: string
  metadata?: Record<string, string>
}

export interface ArchitectureGraphViewModel {
  nodes: ArchitectureGraphNode[]
  edges: ArchitectureGraphEdge[]
}
