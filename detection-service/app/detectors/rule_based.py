class RuleBasedDetector:

    def __init__(self, z_threshold: float = 3.0, velocity_multiplier: float = 5.0):
        self.z_threshold = z_threshold
        self.velocity_multiplier = velocity_multiplier

    def detect(self, features: dict) -> tuple[float, list[str], list[dict]]:
        score = 0.0
        signals = []
        explanations = []

        if features.get("tx_count", 0) > 50:
            score += 0.3
            signals.append("high_transaction_count")
            explanations.append({
                "factor": "Transaction Volume",
                "weight": 0.3,
                "description": f"Agent has {features['tx_count']} transactions, well above typical range",
                "evidence": {"tx_count": features["tx_count"], "threshold": 50}
            })

        if features.get("unique_counterparties", 0) == 1 and features.get("tx_count", 0) > 10:
            score += 0.4
            signals.append("single_counterparty_high_volume")
            explanations.append({
                "factor": "Concentrated Activity",
                "weight": 0.4,
                "description": f"All {features['tx_count']} transactions with single counterparty",
                "evidence": {"counterparties": 1, "tx_count": features["tx_count"]}
            })

        amount_mean = features.get("tx_amount_mean", 0)
        amount_std = features.get("tx_amount_std", 0)
        if amount_mean > 0 and amount_std > 0:
            z_score = amount_std / amount_mean
            if z_score > self.z_threshold:
                score += 0.3
                signals.append("amount_variance_anomaly")
                explanations.append({
                    "factor": "Amount Variance",
                    "weight": 0.3,
                    "description": f"Transaction amount variance (z={z_score:.2f}) exceeds threshold",
                    "evidence": {"z_score": z_score, "threshold": self.z_threshold}
                })

        in_degree = features.get("in_degree", 0)
        out_degree = features.get("out_degree", 0)
        if out_degree > 0 and in_degree == 0:
            score += 0.2
            signals.append("outbound_only")
            explanations.append({
                "factor": "One-Way Flow",
                "weight": 0.2,
                "description": "Agent only sends, never receives - possible fund drain",
                "evidence": {"in_degree": 0, "out_degree": out_degree}
            })

        if in_degree > 0 and out_degree == 0:
            score += 0.2
            signals.append("inbound_only")
            explanations.append({
                "factor": "One-Way Flow",
                "weight": 0.2,
                "description": "Agent only receives, never sends - possible fund sink",
                "evidence": {"in_degree": in_degree, "out_degree": 0}
            })

        reuse = features.get("mandate_reuse_count", 0)
        if reuse > 5:
            score += 0.4
            signals.append("high_mandate_reuse")
            explanations.append({
                "factor": "Mandate Reuse",
                "weight": 0.4,
                "description": f"Mandate reused {reuse} times - possible replay attack",
                "evidence": {"reuse_count": reuse, "threshold": 5}
            })

        in_out = features.get("in_out_ratio", 0)
        if in_out > 10 or (in_out > 0 and in_out < 0.1):
            score += 0.2
            signals.append("skewed_io_ratio")
            explanations.append({
                "factor": "Skewed I/O Ratio",
                "weight": 0.2,
                "description": f"In/out ratio of {in_out:.2f} indicates asymmetric behavior",
                "evidence": {"in_out_ratio": in_out}
            })

        return min(score, 1.0), signals, explanations
