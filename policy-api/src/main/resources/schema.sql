CREATE TABLE IF NOT EXISTS alerts (
    alert_id UUID PRIMARY KEY,
    tx_id UUID,
    agent_id UUID,
    risk_level VARCHAR(16) NOT NULL,
    risk_score DOUBLE PRECISION NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL,
    resolved BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS mandate_holds (
    hold_id UUID PRIMARY KEY,
    mandate_id UUID NOT NULL,
    agent_id UUID,
    status VARCHAR(32) NOT NULL,
    reason TEXT,
    held_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_alerts_agent ON alerts(agent_id);
CREATE INDEX IF NOT EXISTS idx_alerts_resolved ON alerts(resolved);
CREATE INDEX IF NOT EXISTS idx_holds_mandate ON mandate_holds(mandate_id);
