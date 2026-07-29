import type { FC } from 'react'
import type { Alert } from '../types'
import { TableSkeleton } from './Skeleton'

interface Props {
  alerts: Alert[]
  loading: boolean
}

const levelColor: Record<string, string> = {
  CRITICAL: 'text-red-400 bg-red-500/10 border-red-500/20',
  HIGH: 'text-orange-400 bg-orange-500/10 border-orange-500/20',
  MEDIUM: 'text-amber-400 bg-amber-500/10 border-amber-500/20',
  LOW: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20',
}

function truncate(id: string) { return id.length > 8 ? `${id.slice(0, 8)}..` : id }

export const AlertsTable: FC<Props> = ({ alerts, loading }) => {
  return (
    <div className="card-glow p-0 overflow-hidden animate-[fade-in_0.7s_ease-out_0.1s_both]">
      <div className="flex items-center justify-between border-b border-slate-800/50 px-5 py-3">
        <div className="flex items-center gap-2">
          <h2 className="text-[11px] font-semibold uppercase tracking-widest text-slate-500">Alerts</h2>
          {alerts.length > 0 && (
            <span className="rounded bg-slate-800 px-2 py-0.5 text-[10px] text-slate-400">{alerts.length}</span>
          )}
        </div>
      </div>
      <div className="overflow-x-auto">
        {loading ? (
          <div className="p-4"><TableSkeleton rows={4} cols={5} /></div>
        ) : alerts.length === 0 ? (
          <div className="flex flex-col items-center gap-2 py-10 text-slate-600">
            <svg className="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10zM12 6v6M12 16h.01" />
            </svg>
            <span className="text-xs">No alerts</span>
          </div>
        ) : (
          <table className="w-full text-xs">
            <thead>
              <tr className="border-b border-slate-800/40 text-left text-[10px] font-medium uppercase tracking-wider text-slate-600">
                <th className="px-4 py-3 font-normal">Alert</th>
                <th className="px-4 py-3 font-normal">Agent</th>
                <th className="px-4 py-3 font-normal">Level</th>
                <th className="px-4 py-3 font-normal">Score</th>
                <th className="px-4 py-3 font-normal hidden md:table-cell">Reason</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/30">
              {alerts.slice(0, 10).map(a => (
                <tr key={a.alertId} className="transition-colors hover:bg-slate-800/30">
                  <td className="px-4 py-2.5 font-mono text-slate-400">{truncate(a.alertId)}</td>
                  <td className="px-4 py-2.5 font-mono text-slate-400">{truncate(a.agentId)}</td>
                  <td className="px-4 py-2.5">
                    <span className={`inline-block rounded border px-1.5 py-0.5 text-[10px] font-medium ${levelColor[a.riskLevel] || levelColor.LOW}`}>
                      {a.riskLevel}
                    </span>
                  </td>
                  <td className="px-4 py-2.5 font-mono text-slate-300">{a.riskScore.toFixed(3)}</td>
                  <td className="px-4 py-2.5 text-slate-500 hidden md:table-cell table-cell-truncate">{a.reason || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
