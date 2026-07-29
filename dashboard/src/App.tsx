import type { FC } from 'react'
import { useDashboard } from './hooks/useDashboard'
import { Header } from './components/Header'
import { StatsGrid } from './components/StatsGrid'
import { GraphCard } from './components/GraphCard'
import { AlertsTable } from './components/AlertsTable'
import { TransactionsTable } from './components/TransactionsTable'

const App: FC = () => {
  const { state, refresh, runSimulation, trainModel } = useDashboard()

  return (
    <div className="min-h-screen bg-slate-950">
      <Header
        status={state.status}
        onRefresh={refresh}
        onSimulate={runSimulation}
        onTrain={trainModel}
        refreshing={state.refreshing}
      />

      <main className="mx-auto max-w-7xl space-y-4 px-4 py-6 sm:px-6">
        <StatsGrid
          totalTransactions={state.stats?.totalTransactions ?? null}
          activeAlerts={state.stats?.activeAlerts ?? null}
          loading={state.loading}
        />

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <GraphCard
            data={state.graphData}
            detections={state.detections}
            loading={state.loading}
          />
          <AlertsTable
            alerts={state.alerts}
            loading={state.loading}
          />
        </div>

        <TransactionsTable
          transactions={state.transactions}
          loading={state.loading}
        />
      </main>
    </div>
  )
}

export default App
