export interface AgentStat {
  agentId: string
  txCount: number
  txAmountMean: number
  txAmountStd: number
  uniqueCounterparties: number
  inDegree: number
  outDegree: number
  inOutRatio: number
  clusteringCoefficient: number
  mandateReuseCount: number
  timeSinceCreationHours: number
}

export interface Transaction {
  txId: string
  fromAgentId: string
  toAgentId: string
  amount: number
  currency: string
  timestamp: string
  isFraudLabel?: boolean
  fraudLabel?: boolean
}

export interface Alert {
  alertId: string
  agentId: string
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  riskScore: number
  reason: string
  createdAt: string
  resolved?: boolean
}

export interface GraphNode {
  id: string
  degree: number
}

export interface GraphEdge {
  source: string
  target: string
  weight: number
}

export interface GraphData {
  nodes: GraphNode[]
  edges: GraphEdge[]
  nodeCount: number
  edgeCount: number
}

export interface DetectionResult {
  agentId: string
  riskScore: number
  isAnomaly: boolean
  signals: string[]
  method: string
  riskLevel: string
  explanations?: Array<{
    factor: string
    weight: number
    description: string
    evidence: Record<string, unknown>
  }>
}

export interface SimResult {
  totalAgents: number
  totalTransactions: number
  simulationTimeMs: number
  fraudRate: number
  scenario?: string
  dataset?: Record<string, string>
}

export interface TrainingStatus {
  status: string
  samplesTrained: number
}

export interface StatsResponse {
  totalTransactions: number
}

export interface DashboardState {
  stats: { totalTransactions: number; activeAlerts: number } | null
  transactions: Transaction[]
  alerts: Alert[]
  graphData: GraphData | null
  detections: DetectionResult[]
  status: string
  loading: boolean
  refreshing: boolean
  error: string | null
}
