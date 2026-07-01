export type EventStormStickyType =
  | 'DOMAIN_EVENT'
  | 'COMMAND'
  | 'AGGREGATE'
  | 'POLICY'
  | 'READ_MODEL'
  | 'EXTERNAL_SYSTEM'
  | 'USER_ROLE'
  | 'HOTSPOT'
  | 'COMMENT'
  | 'UNKNOWN'

export interface EventStormSticky {
  id: string
  type: EventStormStickyType
  text: string
  position: { x: number; y: number }
  width: number
  height: number
  colour: string
  provenance: 'human-authored' | 'vision-imported' | 'ai-inferred'
  confidence: number
  metadata?: Record<string, string>
}

export interface EventStormBoard {
  id: string
  name: string
  boundedContextHint?: string
  source: 'tldraw' | 'image-import' | 'miro-import'
  capturedAt: string
  stickies: EventStormSticky[]
}
