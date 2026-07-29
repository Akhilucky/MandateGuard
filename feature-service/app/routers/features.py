from fastapi import APIRouter, HTTPException
import httpx
import networkx as nx
import numpy as np
from uuid import UUID
from datetime import datetime, timedelta

from app.models import AgentFeatures, WindowFeatures

router = APIRouter()

LEDGER_URL = "http://ledger:8082/ledger"


@router.get("/{agent_id}/features", response_model=AgentFeatures)
async def get_agent_features(agent_id: UUID):
    async with httpx.AsyncClient() as client:
        resp = await client.get(f"{LEDGER_URL}/transactions/agent/{agent_id}", params={"limit": 500})
        if resp.status_code != 200:
            raise HTTPException(status_code=502, detail="Failed to fetch from ledger")
        txs = resp.json()

    if not txs:
        raise HTTPException(status_code=404, detail="No transactions found")

    return _compute_agent_features(agent_id, txs)


@router.get("/window", response_model=WindowFeatures)
async def get_window_features(
    window_hours: int = 1,
):
    cutoff = datetime.utcnow() - timedelta(hours=window_hours)

    async with httpx.AsyncClient() as client:
        resp = await client.get(f"{LEDGER_URL}/transactions/recent", params={"limit": 10000})
        if resp.status_code != 200:
            raise HTTPException(status_code=502, detail="Failed to fetch from ledger")
        txs = resp.json()

    filtered = [tx for tx in txs if datetime.fromisoformat(tx["timestamp"].replace("Z", "+00:00")).replace(tzinfo=None) >= cutoff]

    G = nx.DiGraph()
    for tx in filtered:
        G.add_edge(tx["from_agent_id"], tx["to_agent_id"], weight=tx["amount"])

    agent_features = []
    for node in G.nodes():
        features = _compute_agent_features_from_graph(node, G, filtered)
        agent_features.append(features)

    return WindowFeatures(
        window_start=cutoff,
        window_end=datetime.utcnow(),
        agent_features=agent_features,
    )


@router.get("/graph")
async def get_graph(window_hours: int = 1):
    cutoff = datetime.utcnow() - timedelta(hours=window_hours)

    async with httpx.AsyncClient() as client:
        resp = await client.get(f"{LEDGER_URL}/transactions/recent", params={"limit": 10000})
        txs = resp.json() if resp.status_code == 200 else []

    filtered = [tx for tx in txs if datetime.fromisoformat(tx["timestamp"].replace("Z", "+00:00")).replace(tzinfo=None) >= cutoff]

    G = nx.DiGraph()
    for tx in filtered:
        G.add_edge(tx["from_agent_id"], tx["to_agent_id"], weight=tx["amount"], tx_id=tx["tx_id"])

    nodes = [{"id": n, "degree": G.degree(n)} for n in G.nodes()]
    edges = [{"source": u, "target": v, "weight": d.get("weight", 0)} for u, v, d in G.edges(data=True)]

    return {"nodes": nodes, "edges": edges, "node_count": len(nodes), "edge_count": len(edges)}


def _compute_agent_features(agent_id: UUID, txs: list) -> AgentFeatures:
    amounts = [tx["amount"] for tx in txs]
    counterparties = set()
    out_degree = 0
    in_degree = 0

    for tx in txs:
        if tx["from_agent_id"] == str(agent_id):
            counterparties.add(tx["to_agent_id"])
            out_degree += 1
        if tx["to_agent_id"] == str(agent_id):
            counterparties.add(tx["from_agent_id"])
            in_degree += 1

    G = nx.DiGraph()
    for tx in txs:
        G.add_edge(tx["from_agent_id"], tx["to_agent_id"])

    try:
        cc = nx.clustering(G.to_undirected(), str(agent_id))
    except Exception:
        cc = 0.0

    mandate_ids = [tx["mandate_id"] for tx in txs]
    mandate_reuse = len(mandate_ids) - len(set(mandate_ids))

    timestamps = [datetime.fromisoformat(tx["timestamp"].replace("Z", "+00:00")).replace(tzinfo=None) for tx in txs]
    oldest = min(timestamps) if timestamps else datetime.utcnow()
    time_since = (datetime.utcnow() - oldest).total_seconds() / 3600

    in_out_ratio = in_degree / out_degree if out_degree > 0 else float(in_degree)

    return AgentFeatures(
        agent_id=agent_id,
        tx_count=len(txs),
        tx_amount_mean=float(np.mean(amounts)) if amounts else 0.0,
        tx_amount_std=float(np.std(amounts)) if amounts else 0.0,
        unique_counterparties=len(counterparties),
        in_degree=in_degree,
        out_degree=out_degree,
        in_out_ratio=in_out_ratio,
        clustering_coefficient=cc,
        mandate_reuse_count=mandate_reuse,
        time_since_creation_hours=time_since,
    )


def _compute_agent_features_from_graph(agent_id: str, G: nx.DiGraph, txs: list) -> AgentFeatures:
    amounts = [tx["amount"] for tx in txs if tx["from_agent_id"] == agent_id or tx["to_agent_id"] == agent_id]
    counterparties = set()
    out_degree = G.out_degree(agent_id) if agent_id in G else 0
    in_degree = G.in_degree(agent_id) if agent_id in G else 0

    if agent_id in G:
        for pred in G.predecessors(agent_id):
            counterparties.add(pred)
        for succ in G.successors(agent_id):
            counterparties.add(succ)

    try:
        cc = nx.clustering(G.to_undirected(), agent_id)
    except Exception:
        cc = 0.0

    mandate_ids = [tx["mandate_id"] for tx in txs if tx["from_agent_id"] == agent_id or tx["to_agent_id"] == agent_id]
    mandate_reuse = len(mandate_ids) - len(set(mandate_ids))

    in_out_ratio = in_degree / out_degree if out_degree > 0 else float(in_degree)

    return AgentFeatures(
        agent_id=UUID(agent_id) if isinstance(agent_id, str) and len(agent_id) == 36 else agent_id,
        tx_count=len(amounts),
        tx_amount_mean=float(np.mean(amounts)) if amounts else 0.0,
        tx_amount_std=float(np.std(amounts)) if amounts else 0.0,
        unique_counterparties=len(counterparties),
        in_degree=in_degree,
        out_degree=out_degree,
        in_out_ratio=in_out_ratio,
        clustering_coefficient=cc,
        mandate_reuse_count=mandate_reuse,
        time_since_creation_hours=0.0,
    )
