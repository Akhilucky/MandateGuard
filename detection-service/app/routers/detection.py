from fastapi import APIRouter, HTTPException
import httpx
from uuid import UUID

from app.models import DetectionResult, AgentFeatures, TrainingStatus
from app.detectors.rule_based import RuleBasedDetector
from app.detectors.ml_detector import MLDetector

router = APIRouter()

rule_detector = RuleBasedDetector()
ml_detector = MLDetector()

FEATURE_SERVICE_URL = "http://feature-service:8083/features"
LEDGER_SERVICE_URL = "http://ledger:8082/ledger"


@router.get("/{agent_id}/risk", response_model=DetectionResult)
async def get_agent_risk(agent_id: UUID):
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get(f"{FEATURE_SERVICE_URL}/{agent_id}/features")
            if resp.status_code == 404:
                raise HTTPException(status_code=404, detail="No data for agent")
            features = resp.json()
    except httpx.ConnectError:
        features = _default_features(str(agent_id))

    rule_score, rule_signals = rule_detector.detect(features)
    ml_score, ml_signals = ml_detector.detect(features)

    combined_score = 0.6 * rule_score + 0.4 * ml_score if ml_detector.trained else rule_score
    all_signals = list(set(rule_signals + ml_signals))

    return DetectionResult(
        agent_id=str(agent_id),
        risk_score=round(combined_score, 4),
        is_anomaly=combined_score > 0.5,
        signals=all_signals,
        method="combined" if ml_detector.trained else "rule_based"
    )


@router.post("/train", response_model=TrainingStatus)
async def train_model():
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get(f"{FEATURE_SERVICE_URL}/window", params={"window_hours": 24})
            if resp.status_code != 200:
                raise HTTPException(status_code=502, detail="Feature service unavailable")
            data = resp.json()
    except httpx.ConnectError:
        raise HTTPException(status_code=502, detail="Feature service unavailable")

    agent_features = data.get("agent_features", [])
    if not agent_features:
        return TrainingStatus(status="no_data", samples_trained=0)

    count = ml_detector.train(agent_features)
    return TrainingStatus(status="trained", samples_trained=count)


@router.get("/batch", response_model=list[DetectionResult])
async def batch_detect(limit: int = 100):
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get(f"{LEDGER_SERVICE_URL}/transactions/recent", params={"limit": limit})
            txs = resp.json() if resp.status_code == 200 else []
    except httpx.ConnectError:
        txs = []

    agent_ids = set()
    for tx in txs:
        agent_ids.add(tx["from_agent_id"])
        agent_ids.add(tx["to_agent_id"])

    results = []
    for aid in list(agent_ids)[:limit]:
        try:
            result = await get_agent_risk(UUID(aid))
            results.append(result)
        except HTTPException:
            continue

    return results


def _default_features(agent_id: str) -> dict:
    return {
        "agent_id": agent_id,
        "tx_count": 0,
        "tx_amount_mean": 0.0,
        "tx_amount_std": 0.0,
        "unique_counterparties": 0,
        "in_degree": 0,
        "out_degree": 0,
        "in_out_ratio": 0.0,
        "clustering_coefficient": 0.0,
        "mandate_reuse_count": 0,
        "time_since_creation_hours": 0.0,
    }
