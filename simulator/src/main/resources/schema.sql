CREATE TABLE IF NOT EXISTS agents (
    agent_id UUID PRIMARY KEY,
    type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    reputation_score DECIMAL(10,4),
    spend_baseline DECIMAL(10,4)
);

CREATE TABLE IF NOT EXISTS mandates (
    mandate_id UUID PRIMARY KEY,
    issuer_agent_id UUID REFERENCES agents(agent_id),
    scope VARCHAR(128),
    max_amount DECIMAL(20,6),
    expiry TIMESTAMP,
    signature VARCHAR(256)
);

CREATE TABLE IF NOT EXISTS transactions (
    tx_id UUID PRIMARY KEY,
    mandate_id UUID REFERENCES mandates(mandate_id),
    from_agent_id UUID NOT NULL,
    to_agent_id UUID NOT NULL,
    amount DECIMAL(20,6) NOT NULL,
    currency VARCHAR(16) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    is_fraud_label BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_tx_from ON transactions(from_agent_id);
CREATE INDEX IF NOT EXISTS idx_tx_to ON transactions(to_agent_id);
CREATE INDEX IF NOT EXISTS idx_tx_mandate ON transactions(mandate_id);
CREATE INDEX IF NOT EXISTS idx_tx_timestamp ON transactions(timestamp);
