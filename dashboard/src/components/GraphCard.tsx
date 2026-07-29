import { type FC, useRef, useEffect, useMemo } from 'react'
import type { GraphData, DetectionResult } from '../types'

interface Props {
  data: GraphData | null
  detections: DetectionResult[]
  loading: boolean
}

interface Position {
  x: number
  y: number
  vx: number
  vy: number
  degree: number
  riskScore: number
}

function simulateForceLayout(nodes: GraphData['nodes'], edges: GraphData['edges'], width: number, height: number): Position[] {
  const positions: Position[] = nodes.map(n => ({
    x: width / 2 + (Math.random() - 0.5) * width * 0.4,
    y: height / 2 + (Math.random() - 0.5) * height * 0.4,
    vx: 0,
    vy: 0,
    degree: n.degree,
    riskScore: 0,
  }))

  const nodeIds = nodes.map(n => n.id)
  const idToIdx = new Map(nodeIds.map((id, i) => [id, i]))

  const maxDegree = Math.max(...positions.map(p => p.degree), 1)
  const repulsion = 4000
  const attraction = 0.003
  const damping = 0.85
  const centerForce = 0.008

  for (let iter = 0; iter < 100; iter++) {
    for (const edge of edges) {
      const fi = idToIdx.get(edge.source)
      const ti = idToIdx.get(edge.target)
      if (fi === undefined || ti === undefined) continue
      const a = positions[fi], b = positions[ti]
      const dx = b.x - a.x, dy = b.y - a.y
      const dist = Math.sqrt(dx * dx + dy * dy) + 0.1
      const force = dist * attraction
      a.vx += (dx / dist) * force
      a.vy += (dy / dist) * force
      b.vx -= (dx / dist) * force
      b.vy -= (dy / dist) * force
    }

    for (let i = 0; i < positions.length; i++) {
      for (let j = i + 1; j < positions.length; j++) {
        const a = positions[i], b = positions[j]
        const dx = b.x - a.x, dy = b.y - a.y
        const distSq = dx * dx + dy * dy + 1
        const force = repulsion / distSq
        const dist = Math.sqrt(distSq)
        a.vx -= (dx / dist) * force
        a.vy -= (dy / dist) * force
        b.vx += (dx / dist) * force
        b.vy += (dy / dist) * force
      }
    }

    for (const p of positions) {
      p.vx += (width / 2 - p.x) * centerForce
      p.vy += (height / 2 - p.y) * centerForce
      p.vx *= damping
      p.vy *= damping
      p.x += p.vx
      p.y += p.vy
      p.x = Math.max(15, Math.min(width - 15, p.x))
      p.y = Math.max(15, Math.min(height - 15, p.y))
    }
  }

  return positions
}

export const GraphCard: FC<Props> = ({ data, detections, loading }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  const detectionMap = useMemo(() => {
    const map = new Map<string, number>()
    for (const d of detections) {
      map.set(d.agentId, d.riskScore)
    }
    return map
  }, [detections])

  const detectionCount = useMemo(() => detections.filter(d => d.isAnomaly).length, [detections])

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || !data || data.nodes.length === 0) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const parent = canvas.parentElement
    const w = parent?.clientWidth ?? 700
    const h = 440
    canvas.width = w * window.devicePixelRatio
    canvas.height = h * window.devicePixelRatio
    canvas.style.width = `${w}px`
    canvas.style.height = `${h}px`
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio)
    ctx.clearRect(0, 0, w, h)

    const positions = simulateForceLayout(data.nodes, data.edges, w, h)
    const maxDegree = Math.max(...positions.map(p => p.degree), 1)

    for (const pos of positions) {
      const nodeId = data.nodes[positions.indexOf(pos)].id
      pos.riskScore = detectionMap.get(nodeId) ?? 0
    }

    if (positions.length === 0) {
      ctx.fillStyle = '#475569'
      ctx.font = '14px system-ui, sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText('No graph data. Run simulation first.', w / 2, h / 2)
      return
    }

    ctx.globalAlpha = 0.12
    ctx.strokeStyle = '#475569'
    ctx.lineWidth = 0.4
    for (const edge of data.edges) {
      const fi = data.nodes.findIndex(n => n.id === edge.source)
      const ti = data.nodes.findIndex(n => n.id === edge.target)
      if (fi === -1 || ti === -1) continue
      const a = positions[fi], b = positions[ti]
      ctx.beginPath()
      ctx.moveTo(a.x, a.y)
      ctx.lineTo(b.x, b.y)
      ctx.stroke()
    }
    ctx.globalAlpha = 1

    for (let i = 0; i < positions.length; i++) {
      const p = positions[i]
      const size = 2 + (p.degree / maxDegree) * 9 + p.riskScore * 4
      const r = p.riskScore > 0.8 ? 239 : p.riskScore > 0.5 ? 249 : p.riskScore > 0.2 ? 251 : 56
      const g = p.riskScore > 0.8 ? 68 : p.riskScore > 0.5 ? 115 : p.riskScore > 0.2 ? 191 : 189
      const b = p.riskScore > 0.8 ? 68 : p.riskScore > 0.5 ? 22 : p.riskScore > 0.2 ? 36 : 248

      ctx.beginPath()
      ctx.arc(p.x, p.y, size, 0, Math.PI * 2)
      ctx.fillStyle = `rgb(${r},${g},${b})`
      ctx.fill()

      if (p.riskScore > 0.5) {
        ctx.beginPath()
        ctx.arc(p.x, p.y, size + 2, 0, Math.PI * 2)
        ctx.strokeStyle = `rgba(239,68,68,${0.2 + p.riskScore * 0.3})`
        ctx.lineWidth = 1.5
        ctx.stroke()
      }
    }
  }, [data, detectionMap])

  return (
    <div className="card-glow overflow-hidden p-0 animate-[fade-in_0.6s_ease-out]">
      <div className="flex items-center justify-between border-b border-slate-800/50 px-5 py-3">
        <div className="flex items-center gap-2">
          <h2 className="text-[11px] font-semibold uppercase tracking-widest text-slate-500">Transaction Graph</h2>
          <span className="rounded bg-slate-800 px-2 py-0.5 text-[10px] text-slate-400">
            {data ? `${data.nodeCount} nodes · ${data.edgeCount} edges` : '—'}
          </span>
        </div>
        {detectionCount > 0 && (
          <span className="rounded-full bg-red-500/10 px-2.5 py-0.5 text-[11px] font-medium text-red-400">
            {detectionCount} flagged
          </span>
        )}
      </div>
      <div className="relative">
        {loading || !data ? (
          <div className="flex h-[440px] items-center justify-center">
            <div className="flex flex-col items-center gap-2 text-slate-600">
              <svg className="h-8 w-8 animate-pulse" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M12 2L2 7l10 5 10-5-10-5z" /><path d="M2 17l10 5 10-5" /><path d="M2 12l10 5 10-5" />
              </svg>
              <span className="text-xs">{loading ? 'Loading graph...' : 'No data'}</span>
            </div>
          </div>
        ) : (
          <canvas ref={canvasRef} className="w-full" />
        )}
      </div>
    </div>
  )
}
