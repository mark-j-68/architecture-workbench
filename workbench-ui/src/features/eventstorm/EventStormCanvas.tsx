import { useCallback, useRef } from 'react'
import { Tldraw, Editor } from 'tldraw'
import 'tldraw/tldraw.css'
import type { EventStormBoard, EventStormStickyType } from './EventStormTypes'

const STICKY_PALETTE: Array<{ type: EventStormStickyType; label: string; colour: string }> = [
  { type: 'DOMAIN_EVENT', label: 'Domain Event', colour: '#f97316' },
  { type: 'COMMAND', label: 'Command', colour: '#facc15' },
  { type: 'AGGREGATE', label: 'Aggregate', colour: '#60a5fa' },
  { type: 'POLICY', label: 'Policy', colour: '#c084fc' },
  { type: 'READ_MODEL', label: 'Read Model', colour: '#4ade80' },
  { type: 'HOTSPOT', label: 'Hotspot', colour: '#f87171' },
]

export interface EventStormCanvasProps {
  boardId: string
  boardName: string
  onBoardExtracted?: (board: EventStormBoard) => void
}

export function EventStormCanvas({ boardId, boardName, onBoardExtracted }: EventStormCanvasProps) {
  const editorRef = useRef<Editor | null>(null)

  const addSticky = useCallback((type: EventStormStickyType, label: string, colour: string) => {
    const editor = editorRef.current
    if (!editor) return

    const viewport = editor.getViewportScreenCenter()
    const pagePoint = editor.screenToPage(viewport)
    const id = `shape:${crypto.randomUUID()}` as any

    editor.createShapes([
      {
        id,
        type: 'geo',
        x: pagePoint.x - 110,
        y: pagePoint.y - 60,
        props: {
          geo: 'rectangle',
          w: 220,
          h: 120,
          color: 'black',
          fill: 'solid',
          text: label,
        },
        meta: { eventStormType: type, eventStormColour: colour },
      } as any,
    ])
    editor.select(id)
  }, [])

  const extractBoard = useCallback(() => {
    const editor = editorRef.current
    if (!editor) return

    const shapes = editor.getCurrentPageShapes()
    const stickies = shapes
      .filter((shape: any) => shape.type === 'geo' || shape.type === 'note')
      .map((shape: any) => ({
        id: String(shape.id),
        type: (shape.meta?.eventStormType ?? 'UNKNOWN') as EventStormStickyType,
        text: String(shape.props?.text ?? ''),
        position: { x: Number(shape.x ?? 0), y: Number(shape.y ?? 0) },
        width: Number(shape.props?.w ?? 220),
        height: Number(shape.props?.h ?? 120),
        colour: String(shape.meta?.eventStormColour ?? ''),
        provenance: 'human-authored' as const,
        confidence: 1.0,
        metadata: {},
      }))

    onBoardExtracted?.({
      id: boardId,
      name: boardName,
      source: 'tldraw',
      capturedAt: new Date().toISOString(),
      stickies,
    })
  }, [boardId, boardName, onBoardExtracted])

  return (
    <section style={{ display: 'grid', gridTemplateRows: 'auto 1fr', height: 'calc(100vh - 120px)', border: '1px solid #30363D', borderRadius: 8, overflow: 'hidden' }}>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', padding: 10, background: '#161B22', borderBottom: '1px solid #30363D' }}>
        {STICKY_PALETTE.map((item) => (
          <button
            key={item.type}
            type="button"
            onClick={() => addSticky(item.type, item.label, item.colour)}
            style={{ border: `1px solid ${item.colour}`, background: 'transparent', color: item.colour, borderRadius: 6, padding: '6px 10px', cursor: 'pointer' }}
          >
            {item.label}
          </button>
        ))}
        <button type="button" onClick={extractBoard} style={{ marginLeft: 'auto', border: '1px solid #58A6FF', background: '#1F3557', color: '#58A6FF', borderRadius: 6, padding: '6px 10px', cursor: 'pointer' }}>
          Extract to Architecture Model
        </button>
      </div>
      <Tldraw persistenceKey={`architecture-workbench:eventstorm:${boardId}`} onMount={(editor) => { editorRef.current = editor }} />
    </section>
  )
}
