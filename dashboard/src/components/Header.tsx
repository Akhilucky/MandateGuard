import type { FC } from 'react'

interface Props {
  status: string
  onRefresh: () => void
  onSimulate: () => void
  onTrain: () => void
  refreshing: boolean
}

export const Header: FC<Props> = ({ status, onRefresh, onSimulate, onTrain, refreshing }) => {
  return (
    <header className="border-b border-slate-800/60 bg-slate-900/40 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-cyan-500/20 to-blue-600/20 ring-1 ring-cyan-500/30">
            <svg className="h-5 w-5 text-cyan-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 2L2 7l10 5 10-5-10-5z" />
              <path d="M2 17l10 5 10-5" />
              <path d="M2 12l10 5 10-5" />
            </svg>
          </div>
          <h1 className="text-lg font-semibold tracking-tight">
            <span className="gradient-text">MandateGuard</span>
          </h1>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 text-xs text-slate-500">
            <span className={`inline-block h-1.5 w-1.5 rounded-full ${status === 'Connected' || status.startsWith('Trained') || status.startsWith('Simulation') ? 'bg-emerald-400' : status === 'Connecting...' ? 'bg-amber-400 animate-pulse' : 'bg-red-400'}`} />
            <span className="hidden sm:inline">{status}</span>
          </div>

          <button onClick={onSimulate} className="rounded-lg border border-slate-700/60 bg-slate-800/50 px-3 py-1.5 text-xs font-medium text-slate-300 transition-all hover:border-cyan-500/40 hover:bg-slate-800 hover:text-cyan-300 active:scale-95">
            Simulate
          </button>
          <button onClick={onTrain} className="rounded-lg border border-slate-700/60 bg-slate-800/50 px-3 py-1.5 text-xs font-medium text-slate-300 transition-all hover:border-violet-500/40 hover:bg-slate-800 hover:text-violet-300 active:scale-95">
            Train ML
          </button>
          <button onClick={onRefresh} disabled={refreshing} className="rounded-lg border border-slate-700/60 bg-slate-800/50 p-1.5 text-slate-500 transition-all hover:border-slate-600 hover:text-slate-300 active:scale-95 disabled:opacity-40">
            <svg className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 2v6h-6M3 12a9 9 0 0115.36-6.36L21 8M3 22v-6h6M21 12a9 9 0 01-15.36 6.36L3 16" />
            </svg>
          </button>
        </div>
      </div>
    </header>
  )
}
