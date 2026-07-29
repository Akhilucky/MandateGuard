import { useState, useEffect, useCallback, useRef } from 'react'
import { api } from '../api'
import type { DashboardState, Transaction, Alert, GraphData, DetectionResult } from '../types'

const EMPTY: DashboardState = {
  stats: null,
  transactions: [],
  alerts: [],
  graphData: null,
  detections: [],
  status: 'Connecting...',
  loading: true,
  refreshing: false,
  error: null,
}

export function useDashboard() {
  const [state, setState] = useState<DashboardState>(EMPTY)
  const mounted = useRef(true)

  const setPartial = useCallback((partial: Partial<DashboardState>) => {
    if (mounted.current) setState(prev => ({ ...prev, ...partial }))
  }, [])

  const refresh = useCallback(async () => {
    setState(prev => ({ ...prev, refreshing: true, error: null }))

    try {
      const [stats, transactions, alerts, graphData, detections] = await Promise.allSettled([
        (async () => {
          const s = await api.ledger.stats()
          let active = 0
          try {
            const a = await api.policy.unresolvedAlerts()
            active = a.length
          } catch {}
          return { totalTransactions: s.totalTransactions, activeAlerts: active }
        })(),
        api.ledger.recentTransactions(20),
        api.policy.alerts(20),
        api.features.graph(24),
        api.detection.batch(200),
      ])

      setState(prev => ({
        ...prev,
        stats: stats.status === 'fulfilled' ? stats.value : prev.stats,
        transactions: transactions.status === 'fulfilled' ? transactions.value : prev.transactions,
        alerts: alerts.status === 'fulfilled' ? alerts.value : prev.alerts,
        graphData: graphData.status === 'fulfilled' ? graphData.value : prev.graphData,
        detections: detections.status === 'fulfilled' ? detections.value : prev.detections,
        refreshing: false,
        loading: false,
        status: 'Connected',
        error: stats.status === 'rejected' ? null : prev.error,
      }))
    } catch (e) {
      setState(prev => ({
        ...prev,
        loading: false,
        refreshing: false,
        status: 'Disconnected',
        error: e instanceof Error ? e.message : 'Connection failed',
      }))
    }
  }, [])

  const runSimulation = useCallback(async () => {
    setState(prev => ({ ...prev, status: 'Running simulation...', error: null }))
    try {
      await api.simulator.run()
      setState(prev => ({ ...prev, status: 'Simulation complete' }))
      await refresh()
    } catch (e) {
      setState(prev => ({
        ...prev,
        status: 'Simulation failed',
        error: e instanceof Error ? e.message : 'Unknown error',
      }))
    }
  }, [refresh])

  const trainModel = useCallback(async () => {
    setState(prev => ({ ...prev, status: 'Training models...', error: null }))
    try {
      const r = await api.detection.train()
      setState(prev => ({ ...prev, status: `Trained: ${r.samplesTrained} samples` }))
    } catch (e) {
      setState(prev => ({
        ...prev,
        status: 'Training failed',
        error: e instanceof Error ? e.message : 'Unknown error',
      }))
    }
  }, [])

  useEffect(() => {
    mounted.current = true
    refresh()
    const interval = setInterval(refresh, 8000)
    return () => {
      mounted.current = false
      clearInterval(interval)
    }
  }, [refresh])

  return { state, refresh, runSimulation, trainModel }
}
