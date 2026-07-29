# MandateGuard

**The first open benchmark for fraud detection in autonomous agent payment networks.**

MandateGuard is a benchmark and simulation framework for evaluating fraud detection algorithms in Agent-to-Agent payment networks using emerging protocols like AP2 and x402.

---

## Why This Matters

As AI agents increasingly transact with each other using mandate-based authorization and HTTP-native stablecoin micropayments, new fraud patterns emerge that traditional card-fraud detection cannot address. No public benchmark exists for this setting. MandateGuard fills that gap.

**Use MandateGuard to:**
- Evaluate your own fraud detection algorithms against labeled synthetic data
- Compare rule-based, ML, and graph neural network approaches on the same dataset
- Study agent-to-agent payment network dynamics at scale
- Generate reproducible benchmark datasets for research

---

## Architecture

```
MandateGuard
├── Traffic Generator        → Generates realistic agent-to-agent transactions
├── Fraud Injector           → Injects labeled fraud across 4 versioned scenarios
├── Benchmark Dataset        → Exports CSV datasets with ground truth labels
└── Detection Algorithms     → Rule-based, Isolation Forest, GNN baselines
```

```
┌──────────────────────┐     ┌──────────────────────┐
│  MandateBench         │────▶│  Ledger Service       │
│  (Traffic Generator   │     │  (Append-only store)  │
│   + Fraud Injector)   │     └──────────┬───────────┘
└──────────────────────┘                 │
                                          ▼
                              ┌──────────────────────┐
                              │ Feature Extraction    │
                              │ (Graph + Temporal)    │
                              └──────────┬───────────┘
                                          ▼
                              ┌──────────────────────┐
                              │ Detection Service     │
                              │ - Rule Baseline       │
                              │ - Isolation Forest    │
                              │ - GNN                 │
                              └──────────┬───────────┘
                                          ▼
                              ┌──────────────────────┐
                              │ Policy / Alert API    │
                              └──────────┬───────────┘
                                          ▼
                              ┌──────────────────────┐
                              │ Dashboard             │
                              └──────────────────────┘
```

---

## Network Profiles

Agents behave differently based on their role in the ecosystem:

| Profile | Scope | Avg Tx/Hour | Avg Amount | Burst Prob |
|---------|-------|-------------|------------|------------|
| Translation Agent | translation-service | 3.0 | $0.50 | 30% |
| Compute Agent | compute-rental | 8.0 | $5.00 | 60% |
| Shopping Agent | shopping-agent | 2.0 | $15.00 | 20% |
| Search Agent | search-agent | 10.0 | $0.10 | 70% |
| Data Broker | data-broker | 1.0 | $50.00 | 40% |
| API Provider | api-provider | 15.0 | $0.20 | 80% |
| Storage Provider | storage-provider | 0.5 | $10.00 | 50% |
| Model Provider | model-provider | 5.0 | $2.00 | 50% |

---

## Fraud Scenarios

Four versioned scenarios for reproducible evaluation:

| Scenario | Name | Fraud Types | Rate |
|----------|------|-------------|------|
| A | Replay Only | Mandate replay | 2% |
| B | Replay + Sybil | Replay, sybil clusters | 3% |
| C | Full Spectrum | All 5 fraud types | 3% |
| D | Targeted | All types, high rate | 5% |

### Fraud Archetypes

1. **Mandate Replay** — Same mandate used for settlement multiple times
2. **Micropayment DoS** — High-frequency sub-cent transaction flooding
3. **Sybil Cluster** — New agent identities transacting only among themselves
4. **Collusion Ring** — Cyclic payment loops with equal amounts
5. **Velocity Anomaly** — Sudden deviation from historical spend baseline

---

## Trust & Reputation

- **Trust Graph**: Agents establish trust relationships; transactions flow over trusted edges
- **Reputation Dynamics**: Scores update based on successful payments, fraud flags, and failures
- **Manipulation**: Fraudsters can attempt to game reputation through sybil/collusion patterns

---

## Benchmark Datasets

Every simulation run exports a complete dataset:

```
dataset/
├── agents.csv          # Agent profiles, types, reputation scores
├── transactions.csv    # Full transaction log with amounts, timestamps
├── trust_graph.csv     # Trust relationships between agents
├── labels.csv          # Ground truth fraud labels per transaction
└── metadata.json       # Scenario, population size, fraud rate
```

### Supported Network Sizes

| Size | Agents | Typical Transactions |
|------|--------|---------------------|
| XS | 100 | ~5K |
| Small | 500 | ~25K |
| Medium | 1,000 | ~50K |
| Large | 3,000 | ~150K |
| XL | 5,000 | ~250K |
| XXL | 10,000 | ~500K |

---

## Detection Algorithms

### Rule Baseline
- Velocity thresholds, mandate reuse checks, I/O ratio analysis
- Deterministic, interpretable, fast

### Isolation Forest (ML)
- Unsupervised anomaly detection on engineered features
- Handles "rare anomaly" framing without labels

### Graph Neural Network (GNN)
- Message-passing aggregation captures graph-structural patterns
- Specifically targets sybil clusters and collusion rings

### Explainability
Every detection result includes:
- **Risk score** (0-1) and **risk level** (LOW/MEDIUM/HIGH/CRITICAL)
- **Contributing factors** with weights and evidence
- **Method** used (rule/ml/gnn/combined)

---

## Quick Start

```bash
docker-compose up --build
```

### Run a Benchmark

```bash
# Run default scenario (C - Full Spectrum, 1000 agents)
curl -X POST http://localhost:8081/simulate/run

# Run specific scenario with custom population
curl -X POST "http://localhost:8081/simulate/run/C?population=5000"

# List available scenarios
curl http://localhost:8081/simulate/scenarios
```

### API Endpoints

| Service | Port | Description |
|---------|------|-------------|
| MandateBench | 8081 | Traffic generator + fraud injector |
| Ledger | 8082 | Append-only transaction store |
| Feature Service | 8083 | Graph & temporal features |
| Detection Service | 8084 | Rule + ML + GNN detection |
| Policy API | 8085 | Alerts, risk scores, holds |
| Dashboard | 3000 | Web UI with graph visualization |

---

## Evaluation

```bash
cd eval
pip install -r requirements.txt

# Run full evaluation (per-archetype metrics)
python metrics.py

# Run scalability evaluation (500-5000 agents)
python metrics.py --scalability

# Or use the Jupyter notebook for visualizations
jupyter notebook evaluation.ipynb
```

### Metrics Reported
- **Per-archetype** precision, recall, F1 (not just aggregate)
- **Detection latency** (transactions until flagged)
- **Scalability** across population sizes
- **ROC-AUC** and precision-recall curves

---

## Project Structure

```
mandateguard/
├── simulator/            # Traffic generator + fraud injector (Java/Spring Boot)
├── ledger/               # Append-only transaction store
├── feature-service/      # Graph & temporal feature extraction (Python/FastAPI)
├── detection-service/    # Rule + ML + GNN detection (Python/FastAPI)
├── policy-api/           # Alerts, risk scores, mandate holds
├── dashboard/            # Web UI with force-directed graph
├── eval/                 # Evaluation scripts + Jupyter notebook
├── docker-compose.yml
└── README.md
```

---

## Tech Stack

- **Java Services**: Java 21, Spring Boot 3.3, Postgres
- **Python Services**: Python 3.11, FastAPI, pandas, networkx, scikit-learn
- **Infrastructure**: Docker Compose
- **CI**: GitHub Actions (Java build, Python lint, Docker build)

---

## For Researchers

MandateGuard is designed to be extended:

1. **Add your own detection algorithm** — implement the detector interface in `detection-service/app/detectors/`
2. **Create new fraud scenarios** — add scenarios in `FraudScenario.java`
3. **Add new agent profiles** — extend `AgentProfile.java` with new behaviors
4. **Export datasets** — every run produces CSV files ready for your experiments

---

## License

Research use. See repository for details.
