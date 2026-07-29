import type { FC } from 'react'

interface StatCardProps {
  label: string
  value: string | number
  accent: string
  icon: string
  loading: boolean
}

const StatCard: FC<StatCardProps> = ({ label, value, accent, icon, loading }) => (
  <div className="card-glow p-5 transition-all duration-300 hover:translate-y-[-1px]">
    <div className="flex items-start justify-between">
      <div>
        <p className="text-[11px] font-medium uppercase tracking-widest text-slate-500">{label}</p>
        {loading ? (
          <div className="mt-2 h-8 w-20 shimmer-bg rounded" />
        ) : (
          <p className={`mt-1 stat-value`} style={{ color: accent }}>{value}</p>
        )}
      </div>
      <div className="flex h-8 w-8 items-center justify-center rounded-lg" style={{ backgroundColor: `${accent}15` }}>
        <span className="text-base" style={{ color: accent }}>{icon}</span>
      </div>
    </div>
  </div>
)

interface Props {
  totalTransactions: number | null
  activeAlerts: number | null
  loading: boolean
}

export const StatsGrid: FC<Props> = ({ totalTransactions, activeAlerts, loading }) => {
  const stats = [
    { label: 'Transactions', value: totalTransactions?.toLocaleString() ?? '—', accent: '#38bdf8', icon: '↗' },
    { label: 'Active Alerts', value: activeAlerts ?? '—', accent: '#ef4444', icon: '⚠' },
    { label: 'Anomaly Rate', value: '—', accent: '#f97316', icon: '■' },
    { label: 'Detection Status', value: 'Active', accent: '#22c55e', icon: '●' },
  ]

  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
      {stats.map(s => (
        <StatCard key={s.label} {...s} loading={loading} />
      ))}
    </div>
  )
}
