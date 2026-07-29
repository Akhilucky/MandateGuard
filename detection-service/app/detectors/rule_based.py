import numpy as np

class RuleBasedDetector:

    def __init__(self, z_threshold: float = 3.0, velocity_multiplier: float = 5.0):
        self.z_threshold = z_threshold
        self.velocity_multiplier = velocity_multiplier

    def detect(self, features: dict) -> tuple[float, list[str]]:
        score = 0.0
        signals = []

        if features.get("tx_count", 0) > 50:
            score += 0.3
            signals.append("high_transaction_count")

        if features.get("unique_counterparties", 0) == 1 and features.get("tx_count", 0) > 10:
            score += 0.4
            signals.append("single_counterparty_high_volume")

        amount_mean = features.get("tx_amount_mean", 0)
        amount_std = features.get("tx_amount_std", 0)
        if amount_mean > 0 and amount_std > 0:
            z_score = amount_std / amount_mean
            if z_score > self.z_threshold:
                score += 0.3
                signals.append("amount_variance_anomaly")

        in_degree = features.get("in_degree", 0)
        out_degree = features.get("out_degree", 0)
        if out_degree > 0 and in_degree == 0:
            score += 0.2
            signals.append("outbound_only")

        if in_degree > 0 and out_degree == 0:
            score += 0.2
            signals.append("inbound_only")

        reuse = features.get("mandate_reuse_count", 0)
        if reuse > 5:
            score += 0.4
            signals.append("high_mandate_reuse")

        in_out = features.get("in_out_ratio", 0)
        if in_out > 10 or (in_out > 0 and in_out < 0.1):
            score += 0.2
            signals.append("skewed_io_ratio")

        return min(score, 1.0), signals
