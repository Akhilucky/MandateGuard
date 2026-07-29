CREATE TABLE IF NOT EXISTS transactions (
    tx_id UUID PRIMARY KEY,
    mandate_id UUID NOT NULL,
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

CREATE OR REPLACE RULE append_only_transactions AS
    ON DELETE TO transactions
    DO INSTEAD NOTHING;

CREATE OR REPLACE RULE no_update_transactions AS
    ON UPDATE TO transactions
    DO INSTEAD NOTHING;
