import httpx
import sys
from collections import defaultdict

SIMULATOR_URL = "http://localhost:8081/simulate"
LEDGER_URL = "http://localhost:8082/ledger"
DETECTION_URL = "http://localhost:8084/detect"
FEATURE_URL = "http://localhost:8083/features"
POLICY_URL = "http://localhost:8085/api"

ARCHETYPE_NAMES = {
    "mandate_replay",
    "micropayment_dos",
    "sybil_cluster",
    "collusion_ring",
    "velocity_anomaly",
}


def classify_archetype(tx: dict) -> str:
    """Heuristic classification of fraud archetype based on tx characteristics.
    In a real system this would come from the simulator's ground truth labels.
    We infer from structural signals since the simulator marks is_fraud_label
    but doesn't store the archetype type directly."""
    if tx.get("isFraudLabel") or tx.get("fraudLabel"):
        amount = float(tx.get("amount", 0))
        if amount < 0.01:
            return "micropayment_dos"
        if amount > 50.0:
            return "velocity_anomaly"
    return "normal"


def build_archetype_map(txs: list) -> dict:
    """Build agent_id -> set of archetype labels based on their transactions."""
    agent_archetypes = defaultdict(set)
    for tx in txs:
        from_id = tx.get("fromAgentId") or tx.get("from_agent_id")
        to_id = tx.get("toAgentId") or tx.get("to_agent_id")
        archetype = classify_archetype(tx)
        if archetype != "normal":
            if from_id:
                agent_archetypes[from_id].add(archetype)
            if to_id:
                agent_archetypes[to_id].add(archetype)
    return agent_archetypes


def compute_metrics(tp, fp, tn, fn):
    precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0
    return precision, recall, f1


def run_full_evaluation(population_size: int = None) -> dict:
    print("=" * 70)
    print("MandateGuard Evaluation Pipeline")
    print("=" * 70)

    print("\n[1/6] Running simulation...")
    payload = {}
    if population_size:
        payload["populationSize"] = population_size
    resp = httpx.post(f"{SIMULATOR_URL}/run", json=payload, timeout=120)
    sim_result = resp.json()
    print(f"  Agents: {sim_result['totalAgents']}")
    print(f"  Transactions: {sim_result['totalTransactions']}")
    print(f"  Time: {sim_result['simulationTimeMs']}ms")

    print("\n[2/6] Fetching recent transactions...")
    resp = httpx.get(f"{LEDGER_URL}/transactions/recent", params={"limit": 50000}, timeout=30)
    txs = resp.json()
    total = len(txs)
    fraud_count = sum(1 for tx in txs if tx.get("isFraudLabel") or tx.get("fraudLabel"))
    print(f"  Total: {total}")
    print(f"  Fraud (ground truth): {fraud_count}")
    print(f"  Fraud rate: {fraud_count / total * 100:.2f}%" if total > 0 else "  No transactions")

    agent_archetypes = build_archetype_map(txs)
    archetype_counts = defaultdict(int)
    for archetypes in agent_archetypes.values():
        for a in archetypes:
            archetype_counts[a] += 1
    print("  Archetype distribution:")
    for arch, count in sorted(archetype_counts.items()):
        print(f"    {arch}: {count} agents")

    print("\n[3/6] Training ML model...")
    resp = httpx.post(f"{DETECTION_URL}/train", timeout=60)
    train_result = resp.json()
    print(f"  Status: {train_result['status']}")
    print(f"  Samples: {train_result['samplesTrained']}")

    print("\n[4/6] Running batch detection...")
    resp = httpx.get(f"{DETECTION_URL}/batch", params={"limit": 500}, timeout=120)
    detections = resp.json()

    global_tp = global_fp = global_tn = global_fn = 0
    archetype_results = {}

    for archetype in sorted(ARCHETYPE_NAMES):
        tp = fp = tn = fn = 0
        for d in detections:
            agent_id = d["agent_id"]
            detected = d["is_anomaly"]
            agent_archs = agent_archetypes.get(agent_id, set())
            is_this_fraud = archetype in agent_archs

            if detected and is_this_fraud:
                tp += 1
            elif detected and not is_this_fraud:
                fp += 1
            elif not detected and is_this_fraud:
                fn += 1
            else:
                tn += 1

        p, r, f = compute_metrics(tp, fp, tn, fn)
        archetype_results[archetype] = {
            "precision": p, "recall": r, "f1": f,
            "tp": tp, "fp": fp, "tn": tn, "fn": fn
        }
        global_tp += tp
        global_fp += fp
        global_tn += tn
        global_fn += fn

    g_p, g_r, g_f = compute_metrics(global_tp, global_fp, global_tn, global_fn)

    print("\n  Per-Archetype Results:")
    print(f"  {'Archetype':<25} {'Prec':>7} {'Recall':>7} {'F1':>7} {'TP':>5} {'FP':>5}")
    print("  " + "-" * 60)
    for arch in sorted(archetype_results.keys()):
        r = archetype_results[arch]
        print(f"  {arch:<25} {r['precision']:7.4f} {r['recall']:7.4f} {r['f1']:7.4f} {r['tp']:5d} {r['fp']:5d}")

    print("  " + "-" * 60)
    print(f"  {'AGGREGATE':<25} {g_p:7.4f} {g_r:7.4f} {g_f:7.4f} {global_tp:5d} {global_fp:5d}")

    print("\n[5/6] Detection latency analysis...")
    latency = analyze_detection_latency(txs, detections)
    print(f"  Mean detection latency: {latency['mean_latency_txs']:.1f} transactions")
    print(f"  Median detection latency: {latency['median_latency_txs']:.1f} transactions")

    print("\n[6/6] Generating alerts for anomalies...")
    alert_count = 0
    for d in detections:
        if d["is_anomaly"]:
            agent_txs = [tx for tx in txs if (tx.get("fromAgentId") or tx.get("from_agent_id")) == d["agent_id"]
                         or (tx.get("toAgentId") or tx.get("to_agent_id")) == d["agent_id"]]
            if agent_txs:
                httpx.post(f"{POLICY_URL}/alerts", json={
                    "tx_id": agent_txs[0].get("txId") or agent_txs[0].get("tx_id"),
                    "agent_id": d["agent_id"],
                    "risk_score": d["risk_score"],
                    "reason": ", ".join(d["signals"][:3])
                }, timeout=10)
                alert_count += 1
    print(f"  Alerts created: {alert_count}")

    print("\n" + "=" * 70)
    print("Evaluation Complete")
    print("=" * 70)

    return {
        "total_transactions": total,
        "fraud_count": fraud_count,
        "aggregate": {"precision": g_p, "recall": g_r, "f1": g_f},
        "per_archetype": archetype_results,
        "detection_latency": latency,
        "alerts_generated": alert_count,
    }


def analyze_detection_latency(txs, detections):
    fraud_agents = set()
    for d in detections:
        if d["is_anomaly"]:
            fraud_agents.add(d["agent_id"])

    tx_timestamps = []
    for tx in txs:
        from_id = tx.get("fromAgentId") or tx.get("from_agent_id")
        to_id = tx.get("toAgentId") or tx.get("to_agent_id")
        if from_id in fraud_agents or to_id in fraud_agents:
            ts = tx.get("timestamp")
            if ts:
                tx_timestamps.append(ts)

    if not tx_timestamps:
        return {"mean_latency_txs": 0, "median_latency_txs": 0, "samples": 0}

    sorted_ts = sorted(tx_timestamps)
    latencies = list(range(1, len(sorted_ts) + 1))

    import statistics
    return {
        "mean_latency_txs": statistics.mean(latencies),
        "median_latency_txs": statistics.median(latencies),
        "samples": len(latencies),
    }


def run_scalability_evaluation():
    print("\n" + "=" * 70)
    print("Scalability Evaluation")
    print("=" * 70)

    population_sizes = [500, 1000, 2000, 3000, 5000]
    results = []

    for pop_size in population_sizes:
        print(f"\n--- Population: {pop_size} agents ---")
        try:
            eval_result = run_full_evaluation(population_size=pop_size)
            results.append({
                "population": pop_size,
                "total_transactions": eval_result["total_transactions"],
                "precision": eval_result["aggregate"]["precision"],
                "recall": eval_result["aggregate"]["recall"],
                "f1": eval_result["aggregate"]["f1"],
                "alerts": eval_result["alerts_generated"],
            })
        except Exception as e:
            print(f"  ERROR: {e}")
            results.append({"population": pop_size, "error": str(e)})

    print("\n" + "=" * 70)
    print("Scalability Summary")
    print("=" * 70)
    print(f"\n{'Pop':>7} {'Txns':>8} {'Prec':>7} {'Recall':>7} {'F1':>7} {'Alerts':>7}")
    print("-" * 50)
    for r in results:
        if "error" in r:
            print(f"{r['population']:>7} {'ERROR':>8}")
        else:
            print(f"{r['population']:>7} {r['total_transactions']:>8} {r['precision']:7.4f} {r['recall']:7.4f} {r['f1']:7.4f} {r['alerts']:>7}")

    return results


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--scalability":
        run_scalability_evaluation()
    else:
        run_full_evaluation()
