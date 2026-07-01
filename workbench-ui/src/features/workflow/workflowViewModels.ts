import type { GraphResponse, ProjectionResponse } from '../../api/architectureApiTypes'
import type { ArchitectureGraphViewModel } from '../graph/ArchitectureGraphTypes'

interface ReactFlowProjectionPayload {
  nodes?: Array<{ id: string; type: string; label: string }>
  edges?: Array<{ id?: string; sourceId: string; targetId: string; relationship: string; label?: string }>
}

export function graphResponseToViewModel(graph: GraphResponse): ArchitectureGraphViewModel {
  return {
    nodes: graph.elements.map((element) => ({
      id: element.id,
      type: element.type,
      label: element.name,
      description: element.description,
      metadata: element.attributes,
    })),
    edges: graph.relationships.map((relationship) => ({
      id: relationship.id,
      sourceId: relationship.sourceId,
      targetId: relationship.targetId,
      relationship: relationship.type,
      label: relationship.label,
      metadata: relationship.attributes,
    })),
  }
}

export function projectionResponseToViewModel(projection: ProjectionResponse | null, fallback: ArchitectureGraphViewModel): ArchitectureGraphViewModel {
  if (!projection || projection.type !== 'REACT_FLOW') return fallback
  const payload = projection.payload as ReactFlowProjectionPayload
  if (!Array.isArray(payload.nodes) || !Array.isArray(payload.edges)) return fallback
  return {
    nodes: payload.nodes.map((node) => ({
      id: node.id,
      type: node.type,
      label: node.label,
    })),
    edges: payload.edges.map((edge) => ({
      id: edge.id,
      sourceId: edge.sourceId,
      targetId: edge.targetId,
      relationship: edge.relationship,
      label: edge.label,
    })),
  }
}
