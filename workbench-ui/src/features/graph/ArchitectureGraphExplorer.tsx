import { useMemo, useState } from 'react'
import {
  Background,
  Controls,
  MiniMap,
  Panel,
  ReactFlow,
  type Edge,
  type Node,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import type { ArchitectureGraphNode, ArchitectureGraphViewModel } from './ArchitectureGraphTypes'

const NODE_STYLE_BY_TYPE: Record<string, { border: string; background: string }> = {
  BOUNDED_CONTEXT: { border: '#58A6FF', background: '#10233F' },
  AGGREGATE: { border: '#60A5FA', background: '#111827' },
  COMMAND: { border: '#FACC15', background: '#2A220B' },
  DOMAIN_EVENT: { border: '#F97316', background: '#2A1608' },
  POLICY: { border: '#C084FC', background: '#241036' },
  READ_MODEL: { border: '#4ADE80', background: '#0D2415' },
  SERVICE: { border: '#94A3B8', background: '#111827' },
  EXTERNAL_SYSTEM: { border: '#F87171', background: '#2A1010' },
  DEPLOYMENT_RESOURCE: { border: '#22D3EE', background: '#08252A' },
  AUDIT_LOG: { border: '#A3E635', background: '#1A2608' },
  AI_JUDGE: { border: '#F0ABFC', background: '#2A0F2F' },
}

export interface ArchitectureGraphExplorerProps {
  graph: ArchitectureGraphViewModel
  onNodeSelected?: (node: ArchitectureGraphNode | null) => void
}

function layout(nodes: ArchitectureGraphNode[]): Node[] {
  const laneOrder = [
    'BOUNDED_CONTEXT',
    'AGGREGATE',
    'COMMAND',
    'DOMAIN_EVENT',
    'POLICY',
    'READ_MODEL',
    'SERVICE',
    'DEPLOYMENT_RESOURCE',
    'EXTERNAL_SYSTEM',
    'AI_JUDGE',
    'AUDIT_LOG',
  ]
  const counts = new Map<string, number>()

  return nodes.map((node) => {
    const lane = Math.max(laneOrder.indexOf(node.type), 0)
    const row = counts.get(node.type) ?? 0
    counts.set(node.type, row + 1)
    const style = NODE_STYLE_BY_TYPE[node.type] ?? { border: '#30363D', background: '#161B22' }

    return {
      id: node.id,
      type: 'default',
      position: { x: lane * 260, y: row * 120 },
      data: {
        label: (
          <div style={{ minWidth: 160 }}>
            <div style={{ fontSize: 10, color: style.border, fontFamily: 'monospace', marginBottom: 4 }}>{node.type}</div>
            <div style={{ fontWeight: 600 }}>{node.label}</div>
            {node.description && <div style={{ fontSize: 11, opacity: 0.75, marginTop: 4 }}>{node.description}</div>}
          </div>
        ),
      },
      style: {
        border: `1px solid ${style.border}`,
        background: style.background,
        color: '#E6EDF3',
        borderRadius: 8,
        padding: 10,
        fontSize: 12,
      },
    }
  })
}

export function ArchitectureGraphExplorer({ graph, onNodeSelected }: ArchitectureGraphExplorerProps) {
  const [filter, setFilter] = useState<string>('ALL')

  const filteredGraph = useMemo(() => {
    if (filter === 'ALL') return graph
    const nodes = graph.nodes.filter((node) => node.type === filter)
    const nodeIds = new Set(nodes.map((node) => node.id))
    return {
      nodes,
      edges: graph.edges.filter((edge) => nodeIds.has(edge.sourceId) && nodeIds.has(edge.targetId)),
    }
  }, [filter, graph])

  const reactFlowNodes = useMemo(() => layout(filteredGraph.nodes), [filteredGraph.nodes])
  const reactFlowEdges = useMemo<Edge[]>(() => filteredGraph.edges.map((edge, index) => ({
    id: edge.id ?? `${edge.sourceId}-${edge.targetId}-${index}`,
    source: edge.sourceId,
    target: edge.targetId,
    label: edge.label ?? edge.relationship,
    animated: edge.relationship.includes('publishes') || edge.relationship.includes('emits'),
    style: { stroke: '#8B949E' },
    labelStyle: { fill: '#8B949E', fontSize: 11 },
  })), [filteredGraph.edges])

  const nodeTypes = useMemo(() => ['ALL', ...Array.from(new Set(graph.nodes.map((node) => node.type))).sort()], [graph.nodes])

  return (
    <section style={{ height: 'calc(100vh - 120px)', border: '1px solid #30363D', borderRadius: 8, overflow: 'hidden', background: '#0D1117' }}>
      <ReactFlow
        colorMode="dark"
        nodes={reactFlowNodes}
        edges={reactFlowEdges}
        fitView
        onNodeClick={(_, node) => onNodeSelected?.(graph.nodes.find((n) => n.id === node.id) ?? null)}
        onPaneClick={() => onNodeSelected?.(null)}
      >
        <Background />
        <Controls />
        <MiniMap nodeStrokeWidth={3} pannable zoomable />
        <Panel position="top-left">
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', background: '#161B22', border: '1px solid #30363D', borderRadius: 8, padding: 8 }}>
            <label htmlFor="graph-filter" style={{ color: '#8B949E', fontSize: 12, fontFamily: 'monospace' }}>Filter</label>
            <select id="graph-filter" value={filter} onChange={(e) => setFilter(e.target.value)} style={{ background: '#0D1117', color: '#E6EDF3', border: '1px solid #30363D', borderRadius: 6, padding: '4px 8px' }}>
              {nodeTypes.map((type) => <option key={type} value={type}>{type}</option>)}
            </select>
          </div>
        </Panel>
      </ReactFlow>
    </section>
  )
}
