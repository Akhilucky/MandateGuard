package com.mandateguard.ledger.repository;

import com.mandateguard.ledger.model.TransactionRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbc;

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(TransactionRecord tx) {
        String sql = "INSERT INTO transactions (tx_id, mandate_id, from_agent_id, to_agent_id, amount, currency, timestamp, is_fraud_label) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbc.update(sql, tx.txId(), tx.mandateId(), tx.fromAgentId(), tx.toAgentId(),
            tx.amount(), tx.currency(), tx.timestamp(), tx.isFraudLabel());
    }

    public Optional<TransactionRecord> findById(UUID txId) {
        String sql = "SELECT * FROM transactions WHERE tx_id = ?";
        List<TransactionRecord> results = jdbc.query(sql, (rs, rowNum) -> new TransactionRecord(
            rs.getObject("tx_id", java.util.UUID.class),
            rs.getObject("mandate_id", java.util.UUID.class),
            rs.getObject("from_agent_id", java.util.UUID.class),
            rs.getObject("to_agent_id", java.util.UUID.class),
            rs.getBigDecimal("amount"),
            rs.getString("currency"),
            rs.getTimestamp("timestamp").toInstant(),
            rs.getBoolean("is_fraud_label")
        ), txId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<TransactionRecord> findByAgent(UUID agentId, int limit) {
        String sql = "SELECT * FROM transactions WHERE from_agent_id = ? OR to_agent_id = ? ORDER BY timestamp DESC LIMIT ?";
        return jdbc.query(sql, (rs, rowNum) -> new TransactionRecord(
            rs.getObject("tx_id", java.util.UUID.class),
            rs.getObject("mandate_id", java.util.UUID.class),
            rs.getObject("from_agent_id", java.util.UUID.class),
            rs.getObject("to_agent_id", java.util.UUID.class),
            rs.getBigDecimal("amount"),
            rs.getString("currency"),
            rs.getTimestamp("timestamp").toInstant(),
            rs.getBoolean("is_fraud_label")
        ), agentId, agentId, limit);
    }

    public List<TransactionRecord> findRecent(int limit) {
        String sql = "SELECT * FROM transactions ORDER BY timestamp DESC LIMIT ?";
        return jdbc.query(sql, (rs, rowNum) -> new TransactionRecord(
            rs.getObject("tx_id", java.util.UUID.class),
            rs.getObject("mandate_id", java.util.UUID.class),
            rs.getObject("from_agent_id", java.util.UUID.class),
            rs.getObject("to_agent_id", java.util.UUID.class),
            rs.getBigDecimal("amount"),
            rs.getString("currency"),
            rs.getTimestamp("timestamp").toInstant(),
            rs.getBoolean("is_fraud_label")
        ), limit);
    }

    public long count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM transactions", Long.class);
    }
}
