from pydantic import BaseModel
from uuid import UUID
from datetime import datetime


class TransactionRecord(BaseModel):
    tx_id: UUID
    mandate_id: UUID
    from_agent_id: UUID
    to_agent_id: UUID
    amount: float
    currency: str
    timestamp: datetime
    is_fraud_label: bool = False


class AgentFeatures(BaseModel):
    agent_id: str
    tx_count: int = 0
    tx_amount_mean: float = 0.0
    tx_amount_std: float = 0.0
    unique_counterparties: int = 0
    in_degree: int = 0
    out_degree: int = 0
    in_out_ratio: float = 0.0
    clustering_coefficient: float = 0.0
    mandate_reuse_count: int = 0
    time_since_creation_hours: float = 0.0


class WindowFeatures(BaseModel):
    window_start: datetime
    window_end: datetime
    agent_features: list[AgentFeatures]


class ExplanationItem(BaseModel):
    factor: str
    weight: float
    description: str
    evidence: dict


class DetectionResult(BaseModel):
    agent_id: str
    risk_score: float
    is_anomaly: bool
    signals: list[str]
    method: str
    risk_level: str = "LOW"
    explanations: list[ExplanationItem] = []


class TrainingStatus(BaseModel):
    status: str
    samples_trained: int
