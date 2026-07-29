import type {
  Transaction,
  Alert,
  GraphData,
  DetectionResult,
  SimResult,
  TrainingStatus,
  StatsResponse,
} from './types'

const BASE = import.meta.env.VITE_API_BASE || ''

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, init)
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
  return res.json()
}

export const api = {
  ledger: {
    recentTransactions: (limit = 50): Promise<Transaction[]> =>
      fetchJson(`${BASE}/ledger/transactions/recent?limit=${limit}`),
    stats: (): Promise<StatsResponse> => fetchJson(`${BASE}/ledger/stats`),
  },
  policy: {
    unresolvedAlerts: (): Promise<Alert[]> => fetchJson(`${BASE}/api/alerts/unresolved`),
    alerts: (limit = 20): Promise<Alert[]> => fetchJson(`${BASE}/api/alerts?limit=${limit}`),
  },
  features: {
    graph: (windowHours = 24): Promise<GraphData> =>
      fetchJson(`${BASE}/features/graph?window_hours=${windowHours}`),
  },
  detection: {
    batch: (limit = 200): Promise<DetectionResult[]> =>
      fetchJson(`${BASE}/detect/batch?limit=${limit}`),
    train: (): Promise<TrainingStatus> =>
      fetchJson(`${BASE}/detect/train`, { method: 'POST' }),
  },
  simulator: {
    run: (): Promise<SimResult> =>
      fetchJson(`${BASE}/simulate/run`, { method: 'POST' }),
  },
}
