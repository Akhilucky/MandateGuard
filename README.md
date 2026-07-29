# MandateGuard

**Fraud & Anomaly Detection for Agent-to-Agent Payment Networks**

A simulation benchmark and baseline evaluation system for detecting fraud in autonomous agent micropayment networks using emerging protocols like AP2 and x402.

---

## Overview

As AI agents increasingly transact with each other using mandate-based authorization and HTTP-native stablecoin micropayments, new fraud patterns emerge that traditional card-fraud detection cannot address. MandateGuard provides:

- A **synthetic transaction simulator** that models realistic agent-to-agent payment networks
- **Five fraud archetypes** specific to agent economies with labeled ground truth
- A **detection pipeline** comparing rule-based and ML-based (Isolation Forest) approaches
- An **evaluation framework** measuring precision, recall, F1, and detection latency per fraud type

## Architecture

```
┌─────────────────────┐      ┌──────────────────────┐
│  Simulator Service   │─────▶│   Ledger Service      │
│  (Java/Spring Boot)  │      │  (Java/Spring Boot +  │
│  - agent population  │      │   Postgres)           │
│  - fraud injection   │      └──────────┬────────────┘
└─────────────────────┘                  │
                                          ▼
                              ┌──────────────────────┐
                              │ Feature Extraction    │
                              │ (Python/FastAPI)      │
                              └──────────┬────────────┘
                                          ▼
                              ┌──────────────────────┐
                              │ Detection Service     │
                              │ (Python/FastAPI)      │
                              │ - rule baseline       │
                              │ - Isolation Forest    │
                              └──────────┬────────────┘
                                          ▼
                              ┌──────────────────────┐
                              │ Policy/Alert API      │
                              │ (Java/Spring Boot)    │
                              └──────────┬────────────┘
                                          ▼
                              ┌──────────────────────┐
                              │ Dashboard (HTML/JS)   │
                              └──────────────────────┘
```

## Fraud Archetypes

| # | Archetype | Signal |
|---|-----------|--------|
| 1 | **Mandate Replay** | Same mandate used for settlement multiple times |
| 2 | **Micropayment DoS** | High-frequency sub-cent transaction flooding |
| 3 | **Sybil Cluster Cash-out** | New agent identities transacting only among themselves |
| 4 | **Collusion / Wash Trading** | Cyclic payment loops with equal amounts |
| 5 | **Velocity Anomaly** | Sudden deviation from historical spend baseline |

## Tech Stack

- **Java Services**: Java 21, Spring Boot 3.3, Postgres
- **Python Services**: Python 3.11, FastAPI, pandas, networkx, scikit-learn
- **Dashboard**: Vanilla HTML/JS
- **Infrastructure**: Docker Compose

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 21+ (for building Spring Boot services)
- Python 3.11+ (for building Python services)

### Run Everything

```bash
docker-compose up --build
```

### Services

| Service | Port | Description |
|---------|------|-------------|
| Simulator | 8081 | Agent population & transaction generator |
| Ledger | 8082 | Append-only transaction store |
| Feature Service | 8083 | Graph & temporal feature extraction |
| Detection Service | 8084 | Rule-based + ML fraud detection |
| Policy API | 8085 | Alerts, risk scores, mandate holds |
| Dashboard | 3000 | Web UI for monitoring |
| PostgreSQL | 5432 | Shared database |

### API Endpoints

**Simulator**
- `POST /simulate/run` - Run a full simulation

**Ledger**
- `GET /ledger/transactions/recent?limit=50` - Recent transactions
- `GET /ledger/transactions/{txId}` - Transaction details
- `GET /ledger/stats` - Transaction count

**Feature Service**
- `GET /features/{agentId}/features` - Per-agent features
- `GET /features/window?window_hours=24` - Windowed features
- `GET /features/graph?window_hours=24` - Transaction graph

**Detection Service**
- `GET /detect/{agentId}/risk` - Agent risk score
- `POST /detect/train` - Train ML model
- `GET /detect/batch?limit=100` - Batch risk scoring

**Policy API**
- `GET /api/alerts` - List alerts
- `POST /api/alerts` - Create alert
- `PUT /api/alerts/{id}/resolve` - Resolve alert
- `POST /api/mandates/{id}/hold` - Hold mandate
- `GET /api/mandates/holds` - List holds

### Run Evaluation

```bash
cd eval
pip install -r requirements.txt
python metrics.py
```

## Project Structure

```
mandateguard/
├── simulator/            # Spring Boot - agent population, fraud injection
├── ledger/               # Spring Boot - append-only transaction log
├── feature-service/      # Python/FastAPI - graph & temporal features
├── detection-service/    # Python/FastAPI - rule + ML detection
├── policy-api/           # Spring Boot - alerts & risk scores
├── dashboard/            # HTML/JS monitoring UI
├── eval/                 # Evaluation scripts & metrics
├── docker-compose.yml
└── README.md
```

## Evaluation Output

The evaluation pipeline reports:
- **Precision, Recall, F1** per fraud archetype (not just aggregate)
- **Detection latency**: transactions/time-steps until flagged
- **False positive rate** at varying population sizes
- **Ablation**: rule-based vs. ML comparison

## License

Research use. See repository for details.
