import type { FC } from 'react'
import type { Transaction } from '../types'
import { TableSkeleton } from './Skeleton'

interface Props {
  transactions: Transaction[]
  loading: boolean
}

function truncate(id: string | undefined) {
  if (!id) return '—'
  return id.length > 8 ? `${id.slice(0, 8)}..` : id
}

function parseAmount(a: number | string): number {
  if (typeof a === 'number') return a
  return parseFloat(a) || 0
}

export const TransactionsTable: FC<Props> = ({ transactions, loading }) => {
  return (
    <div className="card-glow p-0 overflow-hidden animate-[fade-in_0.7s_ease-out_0.2s_both]">
      <div className="flex items-center justify-between border-b border-slate-800/50 px-5 py-3">
        <div className="flex items-center gap-2">
          <h2 className="text-[11px] font-semibold uppercase tracking-widest text-slate-500">Transactions</h2>
          {transactions.length > 0 && (
            <span className="rounded bg-slate-800 px-2 py-0.5 text-[10px] text-slate-400">{transactions.length}</span>
          )}
        </div>
      </div>
      <div className="overflow-x-auto">
        {loading ? (
          <div className="p-4"><TableSkeleton rows={5} cols={5} /></div>
        ) : transactions.length === 0 ? (
          <div className="flex flex-col items-center gap-2 py-10 text-slate-600">
            <svg className="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M12 2v20M2 12h20" />
            </svg>
            <span className="text-xs">No transactions</span>
          </div>
        ) : (
          <table className="w-full text-xs">
            <thead>
              <tr className="border-b border-slate-800/40 text-left text-[10px] font-medium uppercase tracking-wider text-slate-600">
                <th className="px-4 py-3 font-normal">Tx</th>
                <th className="px-4 py-3 font-normal">From</th>
                <th className="px-4 py-3 font-normal">To</th>
                <th className="px-4 py-3 font-normal text-right">Amount</th>
                <th className="px-4 py-3 font-normal text-center">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/30">
              {transactions.slice(0, 12).map(tx => {
                const isFraud = !!(tx.isFraudLabel || tx.fraudLabel)
                const amt = parseAmount(tx.amount)
                return (
                  <tr key={tx.txId} className="transition-colors hover:bg-slate-800/30">
                    <td className="px-4 py-2.5 font-mono text-slate-400">{truncate(tx.txId)}</td>
                    <td className="px-4 py-2.5 font-mono text-slate-400">{truncate(tx.fromAgentId)}</td>
                    <td className="px-4 py-2.5 font-mono text-slate-400">{truncate(tx.toAgentId)}</td>
                    <td className="px-4 py-2.5 text-right font-mono text-slate-300">{amt.toFixed(4)}</td>
                    <td className="px-4 py-2.5 text-center">
                      {isFraud ? (
                        <span className="inline-block rounded bg-red-500/10 px-1.5 py-0.5 text-[10px] font-medium text-red-400">Fraud</span>
                      ) : (
                        <span className="inline-block rounded bg-emerald-500/10 px-1.5 py-0.5 text-[10px] font-medium text-emerald-400">Normal</span>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
