from pydantic import BaseModel
from uuid import UUID
from datetime import datetime

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

class DetectionResult(BaseModel):
    agent_id: str
    risk_score: float
    is_anomaly: bool
    signals: list[str]
    method: str

class TrainingStatus(BaseModel):
    status: str
    samples_trained: int
