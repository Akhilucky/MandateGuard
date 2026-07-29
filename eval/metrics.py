import httpx
import json
import time
from uuid import UUID

SIMULATOR_URL = "http://localhost:8081/simulate"
LEDGER_URL = "http://localhost:8082/ledger"
DETECTION_URL = "http://localhost:8084/detect"
FEATURE_URL = "http://localhost:8083/features"
POLICY_URL = "http://localhost:8085/api"


def run_full_evaluation():
    print("=" * 60)
    print("MandateGuard Evaluation Pipeline")
    print("=" * 60)

    print("\n[1/5] Running simulation...")
    resp = httpx.post(f"{SIMULATOR_URL}/run", timeout=120)
    sim_result = resp.json()
    print(f"  Agents: {sim_result['totalAgents']}")
    print(f"  Transactions: {sim_result['totalTransactions']}")
    print(f"  Time: {sim_result['simulationTimeMs']}ms")

    print("\n[2/5] Fetching recent transactions...")
    resp = httpx.get(f"{LEDGER_URL}/transactions/recent", params={"limit": 10000}, timeout=30)
    txs = resp.json()
    total = len(txs)
    fraud = sum(1 for tx in txs if tx.get("isFraudLabel") or tx.get("fraudLabel"))
    print(f"  Total: {total}")
    print(f"  Fraud (ground truth): {fraud}")
    print(f"  Fraud rate: {fraud/total*100:.2f}%" if total > 0 else "  No transactions")

    print("\n[3/5] Training ML model...")
    resp = httpx.post(f"{DETECTION_URL}/train", timeout=60)
    train_result = resp.json()
    print(f"  Status: {train_result['status']}")
    print(f"  Samples: {train_result['samplesTrained']}")

    print("\n[4/5] Running batch detection...")
    resp = httpx.get(f"{DETECTION_URL}/batch", params={"limit": 200}, timeout=120)
    detections = resp.json()

    tp = fp = tn = fn = 0
    for d in detections:
        agent_id = d["agent_id"]
        detected = d["is_anomaly"]

        agent_txs = [tx for tx in txs if tx["fromAgentId"] == agent_id or tx["toAgentId"] == agent_id]
        is_actually_fraud = any(tx.get("isFraudLabel") or tx.get("fraudLabel") for tx in agent_txs)

        if detected and is_actually_fraud:
            tp += 1
        elif detected and not is_actually_fraud:
            fp += 1
        elif not detected and is_actually_fraud:
            fn += 1
        else:
            tn += 1

    precision = tp / (tp + fp) if (tp + fp) > 0 else 0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0

    print(f"\n  Detection Results:")
    print(f"  True Positives:  {tp}")
    print(f"  False Positives: {fp}")
    print(f"  True Negatives:  {tn}")
    print(f"  False Negatives: {fn}")
    print(f"  Precision:       {precision:.4f}")
    print(f"  Recall:          {recall:.4f}")
    print(f"  F1 Score:        {f1:.4f}")

    print("\n[5/5] Generating alerts for anomalies...")
    alert_count = 0
    for d in detections:
        if d["is_anomaly"]:
            agent_txs = [tx for tx in txs if tx["fromAgentId"] == d["agent_id"] or tx["toAgentId"] == d["agent_id"]]
            if agent_txs:
                httpx.post(f"{POLICY_URL}/alerts", json={
                    "tx_id": agent_txs[0]["txId"],
                    "agent_id": d["agent_id"],
                    "risk_score": d["risk_score"],
                    "reason": ", ".join(d["signals"][:3])
                }, timeout=10)
                alert_count += 1

    print(f"  Alerts created: {alert_count}")

    print("\n" + "=" * 60)
    print("Evaluation Complete")
    print("=" * 60)

    return {
        "total_transactions": total,
        "fraud_count": fraud,
        "precision": precision,
        "recall": recall,
        "f1": f1,
        "alerts_generated": alert_count
    }


if __name__ == "__main__":
    run_full_evaluation()
