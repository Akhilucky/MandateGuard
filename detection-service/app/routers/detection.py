from fastapi import APIRouter, HTTPException
import httpx
from uuid import UUID

from app.models import DetectionResult, AgentFeatures, TrainingStatus, ExplanationItem
from app.detectors.rule_based import RuleBasedDetector
from app.detectors.ml_detector import MLDetector
from app.detectors.gnn_detector import GNNDetector

router = APIRouter()

rule_detector = RuleBasedDetector()
ml_detector = MLDetector()
gnn_detector = GNNDetector()

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

    rule_score, rule_signals, rule_explanations = rule_detector.detect(features)
    ml_score, ml_signals, ml_explanations = ml_detector.detect(features)

    graph_data = None
    try:
        async with httpx.AsyncClient() as client:
            gresp = await client.get(f"{FEATURE_SERVICE_URL}/graph", params={"window_hours": 24})
            if gresp.status_code == 200:
                graph_data = gresp.json()
    except httpx.ConnectError:
        pass

    gnn_score, gnn_signals, gnn_explanations = gnn_detector.detect(features, graph_data)

    scores = [rule_score]
    weights = [0.4]
    if ml_detector.trained:
        scores.append(ml_score)
        weights.append(0.3)
    if gnn_detector.trained:
        scores.append(gnn_score)
        weights.append(0.3)

    total_weight = sum(weights)
    combined_score = sum(s * w for s, w in zip(scores, weights)) / total_weight

    all_signals = list(set(rule_signals + ml_signals + gnn_signals))

    explanations = [ExplanationItem(**e) for e in rule_explanations + ml_explanations + gnn_explanations]

    if combined_score > 0.8:
        risk_level = "CRITICAL"
    elif combined_score > 0.5:
        risk_level = "HIGH"
    elif combined_score > 0.3:
        risk_level = "MEDIUM"
    else:
        risk_level = "LOW"

    methods = ["rule"]
    if ml_detector.trained:
        methods.append("ml")
    if gnn_detector.trained:
        methods.append("gnn")

    return DetectionResult(
        agent_id=str(agent_id),
        risk_score=round(combined_score, 4),
        is_anomaly=combined_score > 0.5,
        signals=all_signals,
        method="+".join(methods),
        risk_level=risk_level,
        explanations=explanations,
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

    ml_count = ml_detector.train(agent_features)

    graph_data = None
    try:
        async with httpx.AsyncClient() as client:
            gresp = await client.get(f"{FEATURE_SERVICE_URL}/graph", params={"window_hours": 24})
            if gresp.status_code == 200:
                graph_data = gresp.json()
    except httpx.ConnectError:
        pass

    gnn_count = gnn_detector.train(agent_features, graph_data)

    return TrainingStatus(status="trained", samples_trained=max(ml_count, gnn_count))


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
        from_id = tx.get("from_agent_id") or tx.get("fromAgentId")
        to_id = tx.get("to_agent_id") or tx.get("toAgentId")
        if from_id:
            agent_ids.add(from_id)
        if to_id:
            agent_ids.add(to_id)

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
