import numpy as np
import joblib
import os
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler

MODEL_DIR = os.environ.get("MODEL_DIR", "/app/models")
MODEL_PATH = os.path.join(MODEL_DIR, "isolation_forest.joblib")
SCALER_PATH = os.path.join(MODEL_DIR, "scaler.joblib")


class MLDetector:

    def __init__(self):
        self.model = None
        self.scaler = StandardScaler()
        self.trained = False
        self._load_model()

    def _load_model(self):
        if os.path.exists(MODEL_PATH) and os.path.exists(SCALER_PATH):
            self.model = joblib.load(MODEL_PATH)
            self.scaler = joblib.load(SCALER_PATH)
            self.trained = True

    def _save_model(self):
        os.makedirs(MODEL_DIR, exist_ok=True)
        joblib.dump(self.model, MODEL_PATH)
        joblib.dump(self.scaler, SCALER_PATH)

    def _features_to_vector(self, features: dict) -> np.ndarray:
        return np.array([
            features.get("tx_count", 0),
            features.get("tx_amount_mean", 0),
            features.get("tx_amount_std", 0),
            features.get("unique_counterparties", 0),
            features.get("in_degree", 0),
            features.get("out_degree", 0),
            features.get("in_out_ratio", 0),
            features.get("clustering_coefficient", 0),
            features.get("mandate_reuse_count", 0),
            features.get("time_since_creation_hours", 0),
        ]).reshape(1, -1)

    def train(self, feature_list: list[dict]) -> int:
        if len(feature_list) < 10:
            return 0

        X = np.array([
            [
                f.get("tx_count", 0),
                f.get("tx_amount_mean", 0),
                f.get("tx_amount_std", 0),
                f.get("unique_counterparties", 0),
                f.get("in_degree", 0),
                f.get("out_degree", 0),
                f.get("in_out_ratio", 0),
                f.get("clustering_coefficient", 0),
                f.get("mandate_reuse_count", 0),
                f.get("time_since_creation_hours", 0),
            ]
            for f in feature_list
        ])

        X_scaled = self.scaler.fit_transform(X)
        self.model = IsolationForest(
            n_estimators=100,
            contamination=0.02,
            random_state=42,
            n_jobs=-1
        )
        self.model.fit(X_scaled)
        self.trained = True
        self._save_model()
        return len(feature_list)

    def detect(self, features: dict) -> tuple[float, list[str], list[dict]]:
        if not self.trained:
            return 0.0, ["model_not_trained"], []

        vec = self._features_to_vector(features)
        vec_scaled = self.scaler.transform(vec)

        prediction = self.model.predict(vec_scaled)[0]
        score_raw = self.model.decision_function(vec_scaled)[0]

        risk_score = max(0.0, min(1.0, 0.5 - score_raw))
        is_anomaly = prediction == -1

        signals = []
        explanations = []
        if is_anomaly:
            signals.append("ml_isolation_forest_anomaly")
            feature_names = [
                "tx_count", "tx_amount_mean", "tx_amount_std",
                "unique_counterparties", "in_degree", "out_degree",
                "in_out_ratio", "clustering_coefficient",
                "mandate_reuse_count", "time_since_creation_hours"
            ]
            vec_values = vec[0]
            for i, name in enumerate(feature_names):
                val = float(vec_values[i])
                if abs(val) > 1.0:
                    explanations.append({
                        "factor": name,
                        "weight": round(abs(val) / max(abs(float(v)) for v in vec_values) + 1e-8, 4),
                        "description": f"Isolation Forest flagged {name}={val:.2f} as anomalous",
                        "evidence": {"feature": name, "value": val}
                    })

        return risk_score, signals, explanations
