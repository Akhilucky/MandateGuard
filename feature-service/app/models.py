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
    agent_id: UUID
    tx_count: int
    tx_amount_mean: float
    tx_amount_std: float
    unique_counterparties: int
    in_degree: int
    out_degree: int
    in_out_ratio: float
    clustering_coefficient: float
    mandate_reuse_count: int
    time_since_creation_hours: float


class WindowFeatures(BaseModel):
    window_start: datetime
    window_end: datetime
    agent_features: list[AgentFeatures]
