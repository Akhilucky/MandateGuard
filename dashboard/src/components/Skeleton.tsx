import type { FC } from 'react'

const SkeletonRow: FC<{ cols: number }> = ({ cols }) => (
  <tr>
    {Array.from({ length: cols }).map((_, i) => (
      <td key={i} className="px-3 py-2.5">
        <div className="h-3 w-16 shimmer-bg rounded" />
      </td>
    ))}
  </tr>
)

export const TableSkeleton: FC<{ rows?: number; cols?: number }> = ({ rows = 5, cols = 5 }) => (
  <table className="w-full">
    <tbody>
      {Array.from({ length: rows }).map((_, i) => (
        <SkeletonRow key={i} cols={cols} />
      ))}
    </tbody>
  </table>
)

export const StatsSkeleton: FC = () => (
  <div className="grid grid-cols-4 gap-4">
    {Array.from({ length: 4 }).map((_, i) => (
      <div key={i} className="card-glow p-5">
        <div className="h-3 w-20 shimmer-bg rounded mb-3" />
        <div className="h-8 w-24 shimmer-bg rounded" />
      </div>
    ))}
  </div>
)
